package com.example.coroutineflow_course.usecases.coroutines.usecase10

import com.example.coroutineflow_course.mock.mockAndroidVersions
import com.example.coroutineflow_course.usecases.coroutines.usecase4.FakeSuccessApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class AndroidVersionRepositoryTest {

    @Test
    fun `getLocalAndroidVersions() should return android versions from database`() = runTest {
        val fakeDatabase = FakeDatabase()

        val repository = AndroidVersionRepository(fakeDatabase, this)
        assertEquals(mockAndroidVersions, repository.getLocalAndroidVersions())
    }

    @Test
    fun `loadRecentAndroidVersions() should return android versions from network`() = runTest {
        val fakeDatabase = FakeDatabase()
        val fakeApi = FakeSuccessApi()
        val repository = AndroidVersionRepository(fakeDatabase, this, fakeApi)

        assertEquals(mockAndroidVersions, repository.loadAndStoreRemoteAndroidVersions())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `loadRecentAndroidVersions() should continue to load and store android versions when calling scope gets cancelled`() =
        runTest {
            val fakeDatabase = FakeDatabase()
            val fakeApi = FakeSuccessApi()
            val applicationScope = this
            val repository = AndroidVersionRepository(fakeDatabase, applicationScope, fakeApi)

            // Sharing the testScheduler with the applicationScope is important!
            val viewModelScope = TestScope(this.testScheduler)
            val job = viewModelScope.launch {
                repository.loadAndStoreRemoteAndroidVersions()
            }

            // execute coroutine until delay(1) in the fakeApi
            applicationScope.runCurrent()

            // Check if nothing is inserted into the db before we cancel the scope
            assertEquals(false, fakeDatabase.insertedIntoDb)

            // Cancel the scope and check if it was indeed cancelled
            viewModelScope.cancel()
            assertEquals(true, job.isCancelled)

            // continue coroutine execution after delay(1) in the fakeApi
            applicationScope.advanceTimeBy(1)
            applicationScope.runCurrent()

            assertEquals(true, fakeDatabase.insertedIntoDb)
        }

}