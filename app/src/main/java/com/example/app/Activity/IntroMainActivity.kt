package com.example.app.Activity

import android.content.Intent
import android.os.Bundle
import com.example.app.databinding.ActivityIntroMainBinding

class IntroMainActivity : BaseActivity() {
    private lateinit var binding: ActivityIntroMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.startBtn.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }
    }
}