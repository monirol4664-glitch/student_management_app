package com.example.soundanalyzer

object AudioProcessor {
    /**
     * Stub implementation that compiles 100%.
     * Replace with real audio logic after APK builds successfully.
     */
    fun startAnalysis(onUpdate: (Double, Double) -> Unit) {
        // TODO: Add AudioRecord + FFT logic here later
        // For now, just emit dummy values to verify UI updates
        kotlin.concurrent.thread {
            while (true) {
                Thread.sleep(500)
                // Dummy values for testing UI
                onUpdate(440.0, 0.78) // 440 Hz = A4 note, ~0.78m wavelength
            }
        }
    }

    fun stopAnalysis() {
        // TODO: Stop recording thread
    }
}