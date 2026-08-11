package com.nichx.unraidassistant.core.di

import com.nichx.unraidassistant.data.repository.DashboardRepository
import com.nichx.unraidassistant.data.repository.DashboardRepositoryImpl
import com.nichx.unraidassistant.data.repository.DockerRepository
import com.nichx.unraidassistant.data.repository.DockerRepositoryImpl
import com.nichx.unraidassistant.data.repository.NotificationRepository
import com.nichx.unraidassistant.data.repository.NotificationRepositoryImpl
import com.nichx.unraidassistant.data.repository.ServerRepository
import com.nichx.unraidassistant.data.repository.ServerRepositoryImpl
import com.nichx.unraidassistant.data.repository.StorageRepository
import com.nichx.unraidassistant.data.repository.StorageRepositoryImpl
import com.nichx.unraidassistant.data.repository.VmRepository
import com.nichx.unraidassistant.data.repository.VmRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindServerRepository(impl: ServerRepositoryImpl): ServerRepository

    @Binds
    @Singleton
    abstract fun bindDashboardRepository(impl: DashboardRepositoryImpl): DashboardRepository

    @Binds
    @Singleton
    abstract fun bindStorageRepository(impl: StorageRepositoryImpl): StorageRepository

    @Binds
    @Singleton
    abstract fun bindDockerRepository(impl: DockerRepositoryImpl): DockerRepository

    @Binds
    @Singleton
    abstract fun bindVmRepository(impl: VmRepositoryImpl): VmRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(impl: NotificationRepositoryImpl): NotificationRepository
}
