package leo.me.la.remote.adapter

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson
import leo.me.la.common.model.Movie
import leo.me.la.common.model.MovieSearchResult
import leo.me.la.exception.OmdbErrorException
import java.rmi.UnexpectedException

internal class MovieSearchAdapter(private val movieAdapter: MovieAdapter) {
    @FromJson
    fun fromJson(
        reader: JsonReader
    ): MovieSearchResult {
        val result = mutableListOf<Movie>()
        var totalResults = 0
        reader.apply {
            beginObject()
            while (hasNext()) {
                when (nextName()) {
                    "Search" -> {
                        beginArray()
                        while (hasNext()) {
                            result.add(movieAdapter.fromJson(this))
                        }
                        endArray()
                    }
                    "totalResults" -> {
                        totalResults = nextInt()
                    }
                    "Error" -> {
                        val errorMessage = nextString()
                        endObject()
                        throw OmdbErrorException(errorMessage)
                    }
                    else -> skipValue()
                }
            }
            endObject()
            if (result.size == 0 || totalResults == 0) {
                throw UnexpectedException("Response misses field(s)")
            } else
                return MovieSearchResult(result, totalResults)
        }
    }

    @Suppress("Unused", "UNUSED_PARAMETER")
    @ToJson
    fun toJson(
        writer: JsonWriter,
        content: MovieSearchResult?
    ) {
        throw UnsupportedOperationException("Cannot deserialize RemoteMovieSearchModel")
    }
}
