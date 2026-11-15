package com.dorai.ai.ui.chat

import app.cash.turbine.test
import com.dorai.ai.data.AutoModelSelection
import com.dorai.ai.data.Service
import com.dorai.ai.data.ServiceEntry
import com.dorai.ai.data.TaskScheduler
import com.dorai.ai.testutil.FakeDataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * "Auto" model selection: the picker entry that selects every model in the provider's Model
 * List so a request falls back across all of them (it is never a model alias).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelAutoModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val unconfinedDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeRepository: FakeDataRepository

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeDataRepository()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ChatViewModel {
        val noOpScheduler = TaskScheduler(fakeRepository, enabled = false)
        return ChatViewModel(fakeRepository, noOpScheduler, unconfinedDispatcher)
    }

    private fun makeServiceEntry(instanceId: String, service: Service) = ServiceEntry(
        instanceId = instanceId,
        serviceId = service.id,
        serviceName = service.displayName,
        modelId = "test-model",
        icon = service.icon,
    )

    private fun selectedModelIds(instanceId: String, service: Service): List<String> = fakeRepository.getInstanceModels(instanceId, service).value.filter { it.isSelected }.map { it.id }

    @Test
    fun `Auto enables list-wide fallback without overwriting the stored selection`() = runTest {
        fakeRepository.setConfiguredServices(Service.LongCat)
        fakeRepository.fakeServiceEntries = listOf(makeServiceEntry("longcat", Service.LongCat))
        val viewModel = createViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertFalse(initialState.isAutoModelSelected, "Auto must start off")

            initialState.actions.selectModel(AutoModelSelection.ID)
            testDispatcher.scheduler.advanceUntilIdle()

            var updated: ChatUiState
            do {
                updated = awaitItem()
            } while (!updated.isAutoModelSelected)
            assertTrue(fakeRepository.getInstanceAutoModelEnabled("longcat"))
            cancelAndIgnoreRemainingEvents()
        }

        // Auto is a selector, not a model: no model was pinned by choosing it.
        assertEquals(emptyList(), selectedModelIds("longcat", Service.LongCat))
    }

    @Test
    fun `Auto keeps the pinned model and leaves it untouched`() = runTest {
        fakeRepository.setConfiguredServices(Service.LongCat)
        fakeRepository.fakeServiceEntries = listOf(makeServiceEntry("longcat", Service.LongCat))
        val viewModel = createViewModel()

        viewModel.state.test {
            skipItems(1)
            viewModel.state.value.actions.selectModel("LongCat-Flash-Thinking")
            testDispatcher.scheduler.advanceUntilIdle()
            val pinned = selectedModelIds("longcat", Service.LongCat)
            assertEquals(listOf("LongCat-Flash-Thinking"), pinned, "precondition: one model pinned")

            viewModel.state.value.actions.selectModel(AutoModelSelection.ID)
            testDispatcher.scheduler.advanceUntilIdle()

            assertTrue(fakeRepository.getInstanceAutoModelEnabled("longcat"))
            assertEquals(
                pinned,
                selectedModelIds("longcat", Service.LongCat),
                "Auto must extend the selection, not replace it",
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pinning a concrete model turns Auto off`() = runTest {
        fakeRepository.setConfiguredServices(Service.LongCat)
        fakeRepository.fakeServiceEntries = listOf(makeServiceEntry("longcat", Service.LongCat))
        val viewModel = createViewModel()

        viewModel.state.test {
            skipItems(1)
            viewModel.state.value.actions.selectModel(AutoModelSelection.ID)
            testDispatcher.scheduler.advanceUntilIdle()
            assertTrue(fakeRepository.getInstanceAutoModelEnabled("longcat"))

            viewModel.state.value.actions.selectModel("LongCat-Flash-Chat")
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(fakeRepository.getInstanceAutoModelEnabled("longcat"))
            assertFalse(viewModel.state.value.isAutoModelSelected)
            assertEquals(listOf("LongCat-Flash-Chat"), selectedModelIds("longcat", Service.LongCat))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Auto is ignored when only the keyless free provider is active`() = runTest {
        val viewModel = createViewModel()

        viewModel.state.test {
            val initialState = awaitItem()
            assertTrue(initialState.availableModels.isEmpty(), "the keyless provider exposes no model list")
            assertEquals(AUTO_FREE_INSTANCE_ID, initialState.availableServices.first().instanceId)

            initialState.actions.selectModel(AutoModelSelection.ID)
            testDispatcher.scheduler.advanceUntilIdle()

            assertFalse(viewModel.state.value.isAutoModelSelected, "there is no Model List to walk")
            cancelAndIgnoreRemainingEvents()
        }
    }

    private companion object {
        const val AUTO_FREE_INSTANCE_ID = "free-auto"
    }
}
