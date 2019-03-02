package leo.me.la.presentation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestCoroutineContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import leo.me.la.common.model.Movie
import leo.me.la.common.model.MovieSearchResult
import leo.me.la.common.model.MovieType
import leo.me.la.domain.SearchMoviesUseCase
import leo.me.la.exception.OmdbErrorException
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule

class SearchViewModelTest {
    @ObsoleteCoroutinesApi
    private val testCoroutineContext = TestCoroutineContext()

    @get:Rule
    var rule: TestRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    private val mainThreadSurrogate = Dispatchers.Unconfined

    private val observer: Observer<SearchViewState> = mockk{
        every { onChanged(any()) } just Runs
    }

    private val useCase: SearchMoviesUseCase = mockk()
    private lateinit var viewModel: SearchViewModel

    @ExperimentalCoroutinesApi
    @Before
    fun setUp() {
        Dispatchers.setMain(mainThreadSurrogate)
    }

    @ExperimentalCoroutinesApi
    @After
    fun tearDown() {
        Dispatchers.resetMain() // reset main dispatcher to the original Main dispatcher
    }

    @Test
    fun `should start in Idling state`() {
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        assertThat(viewModel.viewStates.value).isEqualTo(SearchViewState.Idling)
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should search successfully and move to MoviesFetched state`() {
        val desiredMovieList = List(3) {
            Movie(
                "Batman Begins",
                "2005",
                "tt0372784",
                MovieType.Movie,
                "https://m.media-amazon.com/images/M/MV5BZmUwNGU2ZmItMmRiNC00MjhlLTg5YWUtODMyNzkxODYzMmZlXkEyXkFqcGdeQXVyNTIzOTk5ODM@._V1_SX300.jpg"
            )
        }

        coEvery {
            useCase.execute("Batman")
        } returns MovieSearchResult(
            desiredMovieList,
            3
        )
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Batman")
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Batman",
                    desiredMovieList,
                    1,
                    1
                )
            )
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should cancel previous search if new search is dispatched`() {
        val cancelledMovieList = listOf(
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        )
        val desiredMovieList = listOf(
            Movie(
                "Batman Begins",
                "2005",
                "tt0372784",
                MovieType.Movie,
                "https://m.media-amazon.com/images/M/MV5BZmUwNGU2ZmItMmRiNC00MjhlLTg5YWUtODMyNzkxODYzMmZlXkEyXkFqcGdeQXVyNTIzOTk5ODM@._V1_SX300.jpg"
            )
        )
        coEvery {
            useCase.execute("Abc")
        } coAnswers {
            delay(1000)
            MovieSearchResult(cancelledMovieList, 1)
        }
        coEvery {
            useCase.execute("Batman")
        } returns MovieSearchResult(desiredMovieList, 1)
        val viewModel = SearchViewModel(useCase, testCoroutineContext)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        testCoroutineContext.advanceTimeBy(500)
        viewModel.searchMovies("Batman")
        testCoroutineContext.advanceTimeBy(1000)

        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Batman",
                    desiredMovieList,
                    1,
                    1
                )
            )
        }

        verify(exactly = 0) {
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Abc",
                    cancelledMovieList,
                    1,
                    1
                )
            )
            observer.onChanged(
                SearchViewState.LoadingNextPage
            )
            observer.onChanged(
                SearchViewState.SearchFailed
            )
            observer.onChanged(
                ofType(SearchViewState.LoadPageFailed::class)
            )
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should cancel next page loading if new search is dispatched`() {
        val firstMovieList = List(10) {
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        }
        val secondMovieList = listOf(
            Movie(
                "Batman Begins",
                "2005",
                "tt0372784",
                MovieType.Movie,
                "https://m.media-amazon.com/images/M/MV5BZmUwNGU2ZmItMmRiNC00MjhlLTg5YWUtODMyNzkxODYzMmZlXkEyXkFqcGdeQXVyNTIzOTk5ODM@._V1_SX300.jpg"
            )
        )
        with(useCase) {
            coEvery { execute("Abc") } returns MovieSearchResult(firstMovieList, 200)
            coEvery { execute("Abc", 2) } coAnswers {
                delay(100)
                throw Exception()
            }
            coEvery { execute("Batman") } returns MovieSearchResult(secondMovieList, 1)
        }
        viewModel = SearchViewModel(useCase, testCoroutineContext)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        testCoroutineContext.advanceTimeBy(10)
        viewModel.loadNextPage()
        testCoroutineContext.advanceTimeBy(50)
        viewModel.searchMovies("Batman")
        testCoroutineContext.advanceTimeBy(100)
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Abc",
                    firstMovieList,
                    1,
                    20
                )
            )
            observer.onChanged(SearchViewState.LoadingNextPage)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Batman",
                    secondMovieList,
                    1,
                    1
                )
            )
        }
        verify(exactly = 0) {
            observer.onChanged(SearchViewState.SearchFailed)
            observer.onChanged(ofType(SearchViewState.LoadPageFailed::class))
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should reset to page 1 if new search is dispatched`() {
        val firstMovieList = List(10) {
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        }
        val secondMovieList = List(10) {
            Movie(
                "Def",
                "2005",
                "tt0372789",
                MovieType.Movie,
                ""
            )
        }
        val newSearchMovieList = listOf(
            Movie(
                "Batman Begins",
                "2005",
                "tt0372784",
                MovieType.Movie,
                "https://m.media-amazon.com/images/M/MV5BZmUwNGU2ZmItMmRiNC00MjhlLTg5YWUtODMyNzkxODYzMmZlXkEyXkFqcGdeQXVyNTIzOTk5ODM@._V1_SX300.jpg"
            )
        )
        with(useCase) {
            coEvery { execute("Abc") } returns MovieSearchResult(firstMovieList, 200)
            coEvery { execute("Abc", 2) } returns MovieSearchResult(secondMovieList, 200)
            coEvery { execute("Batman") } returns MovieSearchResult(newSearchMovieList, 1)
        }
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        viewModel.loadNextPage()
        viewModel.searchMovies("Batman")
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Abc",
                    firstMovieList,
                    1,
                    20
                )
            )
            observer.onChanged(SearchViewState.LoadingNextPage)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Abc",
                    secondMovieList,
                    2,
                    20
                )
            )
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(
                SearchViewState.MoviesFetched(
                    "Batman",
                    newSearchMovieList,
                    1,
                    1
                )
            )
        }
        verify(exactly = 0) {
            observer.onChanged(SearchViewState.SearchFailed)
            observer.onChanged(ofType(SearchViewState.LoadPageFailed::class))
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should move to MovieNotFound state`() {
        coEvery { useCase.execute(any(), any()) } throws OmdbErrorException("Movie not found!")
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(SearchViewState.MovieNotFound)
        }
        verify(exactly = 0) {
            observer.onChanged(SearchViewState.SearchFailed)
            observer.onChanged(ofType(SearchViewState.LoadPageFailed::class))
            observer.onChanged(ofType(SearchViewState.MoviesFetched::class))
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should move to SearchFailed state`() {
        with(useCase) {
            coEvery { execute("Abc") } throws OmdbErrorException("empty")
            coEvery { execute("Def") } throws Exception()
        }
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        viewModel.searchMovies("Def")
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(SearchViewState.SearchFailed)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(SearchViewState.SearchFailed)
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should load next page successfully`() {
        val firstMovieList = List(10) {
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        }
        val secondMovieList = List(10) {
            Movie(
                "Def",
                "2001",
                "tt037278e",
                MovieType.Series,
                ""
            )
        }
        with(useCase) {
            coEvery { execute("Abc") } returns MovieSearchResult(firstMovieList, 200)
            coEvery { execute("Abc", 2) } returns MovieSearchResult(secondMovieList, 200)
        }
        viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        viewModel.loadNextPage()
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(ofType(SearchViewState.MoviesFetched::class))
            observer.onChanged(SearchViewState.LoadingNextPage)
            observer.onChanged(ofType(SearchViewState.MoviesFetched::class))
        }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `shouldn't load next page at all if total result is less than 10`() {
        val firstMovieList = List(3) {
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        }
        coEvery {
            useCase.execute("Abc")
        } returns MovieSearchResult(firstMovieList, 3)
        val viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        viewModel.loadNextPage()
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(ofType(SearchViewState.MoviesFetched::class))
        }
        verify(exactly = 0) {
            observer.onChanged(SearchViewState.LoadingNextPage)
        }
        coVerify { useCase.execute(any(), any()) }
    }

    @ObsoleteCoroutinesApi
    @Test
    fun `should move to LoadPageFailed state`() {
        val firstMovieList = List(10) {
            Movie(
                "Abc",
                "2001",
                "tt0372781",
                MovieType.Series,
                ""
            )
        }
        coEvery {
            useCase.execute("Abc")
        } returns MovieSearchResult(firstMovieList, 200)
        coEvery {
            useCase.execute("Abc", 2)
        } throws Exception()
        val viewModel = SearchViewModel(useCase)
        viewModel.viewStates.observeForever(observer)
        viewModel.searchMovies("Abc")
        viewModel.loadNextPage()
        viewModel.loadNextPage()
        verifySequence {
            observer.onChanged(SearchViewState.Idling)
            observer.onChanged(SearchViewState.Searching)
            observer.onChanged(ofType(SearchViewState.MoviesFetched::class))
            observer.onChanged(SearchViewState.LoadingNextPage)
            observer.onChanged(ofType(SearchViewState.LoadPageFailed::class))
            observer.onChanged(SearchViewState.LoadingNextPage)
            observer.onChanged(ofType(SearchViewState.LoadPageFailed::class))
        }
        coVerify(exactly = 2) {
            useCase.execute("Abc", 2)
        }
    }
}