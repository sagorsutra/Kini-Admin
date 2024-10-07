package com.smartherd.kiniadmin.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.smartherd.kiniadmin.R
import com.smartherd.kiniadmin.databinding.ActivityManageBinding
import com.smartherd.kiniadmin.fragment.HomeFragment
import com.smartherd.kiniadmin.fragment.ProductFragment
import com.smartherd.kiniadmin.fragment.SettingsFragment
import com.smartherd.kiniadmin.fragment.UserFragment

class ManageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        replacefragment(HomeFragment())

        binding.bottomNavigation.setOnItemSelectedListener {
            when(it.itemId){
                R.id.homeFragment ->replacefragment(HomeFragment())
                R.id.productFragment ->replacefragment(ProductFragment())
                R.id.userFragment ->replacefragment(UserFragment())
                R.id.profileFragment ->replacefragment(SettingsFragment())

            }
            true
        }

    }

    private fun replacefragment(fragment: Fragment){
        val fragmentManager = supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(R.id.frame_layout,fragment)
        fragmentTransaction.commit()
    }
}