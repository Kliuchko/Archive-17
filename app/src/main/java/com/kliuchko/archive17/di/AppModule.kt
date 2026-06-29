package com.kliuchko.archive17.di

import androidx.room.Room
import com.kliuchko.archive17.core.time.SystemTimeProvider
import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.Archive17Database
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.repository.DefaultBookRepository
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.presentation.search.SearchViewModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

val appModule = module {
    single<TimeProvider> { SystemTimeProvider }

    single {
        Room.databaseBuilder(
            androidContext(),
            Archive17Database::class.java,
            Archive17Database.DATABASE_NAME,
        ).build()
    }

    single { get<Archive17Database>().workDao() }
    single { get<Archive17Database>().editionDao() }
    single { get<Archive17Database>().libraryEntryDao() }

    single {
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
    }

    single {
        OkHttpClient.Builder()
            .addInterceptor(get<HttpLoggingInterceptor>())
            .build()
    }

    single {
        Retrofit.Builder()
            .baseUrl(OpenLibraryApi.BASE_URL)
            .client(get())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    single<OpenLibraryApi> {
        get<Retrofit>().create(OpenLibraryApi::class.java)
    }

    single<BookRepository> {
        DefaultBookRepository(
            api = get(),
            workDao = get(),
            libraryEntryDao = get(),
            timeProvider = get(),
        )
    }

    viewModel {
        SearchViewModel(repository = get())
    }
}
