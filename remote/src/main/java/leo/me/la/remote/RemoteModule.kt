package leo.me.la.remote

import com.squareup.moshi.Moshi
import leo.me.la.common.TAG_BOOLEAN_DEBUG
import leo.me.la.common.TAG_INTERCEPTOR_API_KEY
import leo.me.la.common.TAG_INTERCEPTOR_LOGGING
import leo.me.la.common.TAG_OMDB_API_KEY
import leo.me.la.data.source.MovieRemoteDataSource
import leo.me.la.remote.adapter.MovieSearchAdapter
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module.module
import retrofit2.converter.moshi.MoshiConverterFactory

val remoteModule = module {

    single {
        RemoteFactory.buildOkHttpClient(
            listOf(get(name = TAG_INTERCEPTOR_API_KEY)),
            listOf(get(name = TAG_INTERCEPTOR_LOGGING))
        )
    }

    single {
        Moshi.Builder()
            .add(MovieSearchAdapter())
            .build()
    }

    factory<MovieRemoteDataSource> {
        MovieRemoteDataSourceImpl(get())
    }

    factory {
        RemoteFactory.buildRestApi(
            "http://www.omdbapi.com/",
            OmdbRestApi::class.java,
            get(),
            get()
        )
    }

    single {
        MoshiConverterFactory.create(get())
    }

    factory<Interceptor>(name = TAG_INTERCEPTOR_LOGGING) {
        val isDebug: Boolean = get(name = TAG_BOOLEAN_DEBUG)
        HttpLoggingInterceptor().apply {
            level = if (isDebug) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
    }

    factory<Interceptor>(name = TAG_INTERCEPTOR_API_KEY) {
        ApiKeyInterceptor(get(name = TAG_OMDB_API_KEY))
    }
}