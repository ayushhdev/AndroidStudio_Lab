package com.example.basicviews

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.basicviews.databinding.ActivitySummaryBinding

/** Shows the values read off the basic Views on [MainActivity]. */
class SummaryActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySummaryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySummaryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(Extras.NAME).orEmpty()

        binding.valueName.text = name.ifEmpty { "Not entered" }
        binding.valueSkills.text = intent.getStringExtra(Extras.SKILLS).orEmpty()
        binding.valueDevice.text = intent.getStringExtra(Extras.DEVICE).orEmpty()
        binding.valueUpdates.text = intent.getBooleanExtra(Extras.UPDATES, false).asOnOff()
        binding.valueTheme.text = intent.getBooleanExtra(Extras.DARK_MODE, false).asOnOff()
        binding.valueSubject.text = intent.getStringExtra(Extras.SUBJECT).orEmpty()
        binding.valueHours.text = "${intent.getIntExtra(Extras.HOURS, 0)} hours"
        binding.valueRating.text = "${intent.getFloatExtra(Extras.RATING, 0f)} / 5"
        binding.valueLikes.text = intent.getIntExtra(Extras.LIKES, 0).toString()

        binding.backButton.setOnClickListener { finish() }
    }

    private fun Boolean.asOnOff() = if (this) "On" else "Off"
}
