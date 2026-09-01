package com.example.basicviews

import android.content.Intent
import android.os.Bundle
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.basicviews.databinding.ActivityMainBinding

/**
 * Experiment 06 — every interactive widget on this screen is a basic View from
 * android.widget. The activity wires them up with listeners and collects their
 * values when Submit is pressed.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var likeCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUpTextAndButton()
        setUpSeekBar()
        setUpLikeButton()
        setUpActions()
    }

    /** Button click reads the EditText and writes into a TextView. */
    private fun setUpTextAndButton() {
        binding.greetButton.setOnClickListener {
            val name = binding.nameInput.text.toString().trim()
            binding.greetingText.text = if (name.isEmpty()) {
                getString(R.string.greeting_empty)
            } else {
                "Hello, $name! Welcome to Experiment 6."
            }
        }
    }

    /** SeekBar reports its progress into the label above it as the thumb moves. */
    private fun setUpSeekBar() {
        updateSeekLabel(binding.hoursSeekBar.progress)
        binding.hoursSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSeekLabel(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun updateSeekLabel(progress: Int) {
        binding.seekLabel.text = getString(R.string.seek_label) + ": $progress h"
    }

    /** ImageButton taps drive both a TextView counter and a ProgressBar. */
    private fun setUpLikeButton() {
        binding.likeButton.setOnClickListener {
            if (likeCount < binding.likeProgress.max) {
                likeCount++
                binding.likeCountText.text = "Likes: $likeCount"
                binding.likeProgress.progress = likeCount
            } else {
                Toast.makeText(this, "That is plenty of likes!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setUpActions() {
        binding.submitButton.setOnClickListener { openSummary() }
        binding.resetButton.setOnClickListener { resetViews() }
    }

    /** Collects the state of every view and passes it to the summary screen. */
    private fun openSummary() {
        val skills = mutableListOf<String>()
        if (binding.checkKotlin.isChecked) skills.add(getString(R.string.skill_kotlin))
        if (binding.checkXml.isChecked) skills.add(getString(R.string.skill_xml))
        if (binding.checkGradle.isChecked) skills.add(getString(R.string.skill_gradle))

        val device = when (binding.deviceGroup.checkedRadioButtonId) {
            R.id.radioTablet -> getString(R.string.device_tablet)
            R.id.radioFoldable -> getString(R.string.device_foldable)
            else -> getString(R.string.device_phone)
        }

        val intent = Intent(this, SummaryActivity::class.java).apply {
            putExtra(Extras.NAME, binding.nameInput.text.toString().trim())
            putExtra(Extras.SKILLS, skills.joinToString(", ").ifEmpty { "None selected" })
            putExtra(Extras.DEVICE, device)
            putExtra(Extras.UPDATES, binding.updatesSwitch.isChecked)
            putExtra(Extras.DARK_MODE, binding.darkToggle.isChecked)
            putExtra(Extras.SUBJECT, binding.subjectSpinner.selectedItem.toString())
            putExtra(Extras.HOURS, binding.hoursSeekBar.progress)
            putExtra(Extras.RATING, binding.ratingBar.rating)
            putExtra(Extras.LIKES, likeCount)
        }
        startActivity(intent)
    }

    /** Puts every view back to the state it had when the screen opened. */
    private fun resetViews() {
        binding.nameInput.text.clear()
        binding.greetingText.setText(R.string.greeting_empty)
        binding.checkKotlin.isChecked = true
        binding.checkXml.isChecked = false
        binding.checkGradle.isChecked = false
        binding.radioPhone.isChecked = true
        binding.updatesSwitch.isChecked = false
        binding.darkToggle.isChecked = false
        binding.subjectSpinner.setSelection(0)
        binding.hoursSeekBar.progress = 6
        binding.ratingBar.rating = 4f
        likeCount = 0
        binding.likeCountText.text = "Likes: 0"
        binding.likeProgress.progress = 0
        Toast.makeText(this, "All views reset", Toast.LENGTH_SHORT).show()
    }
}
