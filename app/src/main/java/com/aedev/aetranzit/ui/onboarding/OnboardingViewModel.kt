package com.aedev.aetranzit.ui.onboarding

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aedev.aetranzit.R
import com.aedev.aetranzit.domain.repository.SyncPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingSlide(
    @DrawableRes val imageRes: Int,
    val title: String,
    val description: String
)

val ONBOARDING_SLIDES = listOf(
    OnboardingSlide(
        imageRes = R.drawable.onboarding_1_ticketing,
        title = "Fast Conductor Ticketing",
        description = "Issue digital tickets in seconds with automatic 20% discount calculations and interactive seat mapping."
    ),
    OnboardingSlide(
        imageRes = R.drawable.onboarding_2_digital_ticket,
        title = "QR Digital Receipts",
        description = "Share official QR digital receipts via SMS, web link, or image share sheet instantly."
    ),
    OnboardingSlide(
        imageRes = R.drawable.onboarding_3_offline_sync,
        title = "Seamless Offline Sync",
        description = "Keep operating in cellular dead zones. Tickets and trip logs automatically sync when connection returns."
    ),
    OnboardingSlide(
        imageRes = R.drawable.onboarding_4_admin_analytics,
        title = "Admin Analytics & Operations",
        description = "Track revenue, occupancy, route performance, and manage fleet & staff seamlessly in real time."
    )
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val syncPreferencesRepository: SyncPreferencesRepository
) : ViewModel() {

    fun completeOnboarding(onCompleted: () -> Unit) {
        viewModelScope.launch {
            syncPreferencesRepository.setOnboardingCompleted(true)
            onCompleted()
        }
    }
}
