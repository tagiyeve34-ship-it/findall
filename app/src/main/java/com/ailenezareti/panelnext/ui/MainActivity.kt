package com.ailenezareti.panelnext.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ailenezareti.panelnext.Prefs
import com.ailenezareti.panelnext.R
import com.ailenezareti.panelnext.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(Prefs.token(this).isBlank()){ startActivity(Intent(this,LoginActivity::class.java)); finish(); return }
        b=ActivityMainBinding.inflate(layoutInflater); setContentView(b.root)
        if(savedInstanceState==null) show(DashboardFragment())
        b.bottomNav.setOnItemSelectedListener { item ->
            when(item.itemId){
                R.id.nav_dashboard -> show(DashboardFragment())
                R.id.nav_map -> show(MapFragment())
                R.id.nav_calls -> show(CallsFragment())
                R.id.nav_zones -> show(ZonesFragment())
                R.id.nav_alerts -> show(AlertsFragment())
                else -> return@setOnItemSelectedListener false
            }; true
        }
    }
    private fun show(f:Fragment){ supportFragmentManager.beginTransaction().replace(R.id.fragmentHost,f).commit() }
}
