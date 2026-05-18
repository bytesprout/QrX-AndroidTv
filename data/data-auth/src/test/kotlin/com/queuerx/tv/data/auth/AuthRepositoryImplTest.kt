package com.queuerx.tv.data.auth

import com.google.common.truth.Truth.assertThat
import com.queuerx.tv.core.common.ApiResult
import com.queuerx.tv.core.storage.InMemoryKeyValueStore
import com.queuerx.tv.domain.auth.AuthToken
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AuthRepositoryImplTest {

    private lateinit var store: InMemoryKeyValueStore
    private lateinit var repo: AuthRepositoryImpl

    private fun token(expiresFromNow: Long = 3_600_000L) = AuthToken(
        accessToken = "access-test",
        refreshToken = "refresh-test",
        expiresAtEpochMillis = System.currentTimeMillis() + expiresFromNow
    )

    @BeforeEach
    fun setUp() {
        store = InMemoryKeyValueStore()
        repo = AuthRepositoryImpl(
            httpClient = mockk(relaxed = true),
            store = store
        )
    }

    @Test
    fun `getToken returns null when nothing persisted`() = runTest {
        assertThat(repo.getToken()).isNull()
    }

    @Test
    fun `saveToken persists and getToken retrieves it`() = runTest {
        val t = token()
        val saveResult = repo.saveToken(t)
        assertThat(saveResult.isSuccess).isTrue()

        val loaded = repo.getToken()
        assertThat(loaded).isNotNull()
        assertThat(loaded!!.accessToken).isEqualTo("access-test")
    }

    @Test
    fun `clearToken removes persisted token`() = runTest {
        repo.saveToken(token())
        repo.clearToken()
        assertThat(repo.getToken()).isNull()
    }

    @Test
    fun `accessToken returns null before any token saved`() {
        assertThat(repo.accessToken()).isNull()
    }

    @Test
    fun `accessToken returns cached value after saveToken`() = runTest {
        repo.saveToken(token())
        assertThat(repo.accessToken()).isEqualTo("access-test")
    }

    @Test
    fun `accessToken returns null after clearToken`() = runTest {
        repo.saveToken(token())
        repo.clearToken()
        assertThat(repo.accessToken()).isNull()
    }

    @Test
    fun `warmCache populates in-memory cache from store`() = runTest {
        // Pre-seed the store directly to simulate a fresh app start
        repo.saveToken(token())

        // Create a new repo instance reading from the same store
        val newRepo = AuthRepositoryImpl(httpClient = mockk(relaxed = true), store = store)
        assertThat(newRepo.accessToken()).isNull() // not warm yet
        newRepo.warmCache()
        assertThat(newRepo.accessToken()).isEqualTo("access-test")
    }
}
