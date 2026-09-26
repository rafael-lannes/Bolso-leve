package com.bolsoleve.app.di

import androidx.room.Room
import com.bolsoleve.app.data.local.BolsoLeveDatabase
import com.bolsoleve.app.data.local.datastore.UserPreferencesRepository
import com.bolsoleve.app.data.repository.DoctorRepositoryImpl
import com.bolsoleve.app.data.repository.ExpenseRepositoryImpl
import com.bolsoleve.app.data.repository.MedicationRepositoryImpl
import com.bolsoleve.app.data.repository.WeightRepositoryImpl
import com.bolsoleve.app.domain.repository.DoctorRepository
import com.bolsoleve.app.domain.repository.ExpenseRepository
import com.bolsoleve.app.domain.repository.MedicationRepository
import com.bolsoleve.app.domain.repository.WeightRepository
import com.bolsoleve.app.domain.usecase.AddMedicationBoxUseCase
import com.bolsoleve.app.domain.usecase.CompleteOnboardingUseCase
import com.bolsoleve.app.domain.usecase.ConfirmDoseApplicationUseCase
import com.bolsoleve.app.domain.usecase.DeleteWeightRecordUseCase
import com.bolsoleve.app.domain.usecase.GetAllWeightRecordsUseCase
import com.bolsoleve.app.domain.usecase.GetDashboardDataUseCase
import com.bolsoleve.app.domain.usecase.GetTreatmentStatsUseCase
import com.bolsoleve.app.domain.usecase.SaveWeightRecordUseCase
import com.bolsoleve.app.domain.usecase.UpdateWeightRecordUseCase
import com.bolsoleve.app.data.backup.BackupManager
import com.bolsoleve.app.presentation.screens.dashboard.DashboardViewModel
import com.bolsoleve.app.presentation.screens.medicine.MedicineViewModel
import com.bolsoleve.app.presentation.screens.onboarding.OnboardingViewModel
import com.bolsoleve.app.presentation.screens.options.OptionsViewModel
import com.bolsoleve.app.presentation.screens.stats.StatsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            BolsoLeveDatabase::class.java,
            BolsoLeveDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration().build()
    }
    single { get<BolsoLeveDatabase>().weightDao() }
    single { get<BolsoLeveDatabase>().medicationDao() }
    single { get<BolsoLeveDatabase>().expenseDao() }
    single { get<BolsoLeveDatabase>().doctorDao() }
    single { UserPreferencesRepository(androidContext()) }
    single { BackupManager(get(), get(), get(), get(), get()) }
}

val repositoryModule = module {
    single<WeightRepository> { WeightRepositoryImpl(get()) }
    single<MedicationRepository> { MedicationRepositoryImpl(get()) }
    single<ExpenseRepository> { ExpenseRepositoryImpl(get()) }
    single<DoctorRepository> { DoctorRepositoryImpl(get()) }
}

val useCaseModule = module {
    factory { GetDashboardDataUseCase(get(), get(), get(), get()) }
    factory { SaveWeightRecordUseCase(get()) }
    factory { UpdateWeightRecordUseCase(get()) }
    factory { DeleteWeightRecordUseCase(get()) }
    factory { GetAllWeightRecordsUseCase(get()) }
    factory { ConfirmDoseApplicationUseCase(get(), get()) }
    factory { GetTreatmentStatsUseCase(get(), get(), get()) }
    factory { AddMedicationBoxUseCase(get(), get(), get()) }
    factory { CompleteOnboardingUseCase(get(), get(), get(), get()) }
}

val viewModelModule = module {
    viewModel { DashboardViewModel(get(), get(), get(), get(), get(), get()) }
    viewModel { MedicineViewModel(get(), get(), get(), get(), get()) }
    viewModel { StatsViewModel(get(), get(), get(), get()) }
    viewModel { OnboardingViewModel(get(), get()) }
    viewModel { OptionsViewModel(get(), get(), get(), get(), get(), get()) }
}

val appModules = listOf(
    databaseModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
