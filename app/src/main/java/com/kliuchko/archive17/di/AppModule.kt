package com.kliuchko.archive17.di

import androidx.room.Room
import com.kliuchko.archive17.BuildConfig
import com.kliuchko.archive17.core.time.SystemTimeProvider
import com.kliuchko.archive17.core.time.TimeProvider
import com.kliuchko.archive17.data.local.Archive17Database
import com.kliuchko.archive17.data.networking.api.OpenLibraryApi
import com.kliuchko.archive17.data.repository.DefaultBookRepository
import com.kliuchko.archive17.data.reader.ReadiumService
import com.kliuchko.archive17.data.repository.DefaultLocalBookRepository
import com.kliuchko.archive17.domain.repository.BookRepository
import com.kliuchko.archive17.domain.repository.LocalBookRepository
import com.kliuchko.archive17.presentation.details.BookDetailsViewModel
import com.kliuchko.archive17.presentation.library.LibraryViewModel
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
        )
            .addMigrations(Archive17Database.MIGRATION_1_2)
            .build()
    }

    single { get<Archive17Database>().workDao() }
    single { get<Archive17Database>().editionDao() }
    single { get<Archive17Database>().libraryEntryDao() }
    single { get<Archive17Database>().localBookDao() }
    single { ReadiumService(androidContext()) }

    single {
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
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

    single<LocalBookRepository> {
        DefaultLocalBookRepository(
            context = androidContext(),
            localBookDao = get(),
            readiumService = get(),
            timeProvider = get(),
        )
    }

    viewModel {
        SearchViewModel(repository = get())
    }

    viewModel { parameters ->
        BookDetailsViewModel(
            workId = parameters.get(),
            repository = get(),
        )
    }

    viewModel {
        LibraryViewModel(
            repository = get(),
            localBookRepository = get(),
        )
    }
}
