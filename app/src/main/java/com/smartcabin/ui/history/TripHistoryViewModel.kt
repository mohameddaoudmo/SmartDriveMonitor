package com.smartcabin.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.smartcabin.data.repository.TripRepositoryImpl
import com.smartcabin.domain.model.Trip
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TripHistoryViewModel @Inject constructor(
    tripRepository: TripRepositoryImpl
) : ViewModel() {

    val allTrips: StateFlow<List<Trip>> = tripRepository.getAllTrips()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
