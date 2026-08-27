package com.ailenezareti.panelnext.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ailenezareti.panelnext.Prefs
import com.ailenezareti.panelnext.api.ApiClient
import com.ailenezareti.panelnext.databinding.ActivityLoginBinding
import com.ailenezareti.panelnext.model.LoginRequest
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var b: ActivityLoginBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Prefs.token(this).isNotBlank()) { startActivity(Intent(this, MainActivity::class.java)); finish(); return }
        b = ActivityLoginBinding.inflate(layoutInflater); setContentView(b.root)
        b.loginButton.setOnClickListener { login() }
    }
    private fun login() {
        val email=b.email.text.toString().trim(); val pass=b.password.text.toString()
        if(email.isBlank()||pass.isBlank()){ show("E-poçt və şifrəni daxil et"); return }
        b.loginButton.isEnabled=false; b.loginButton.text="Yoxlanılır…"; b.error.visibility=View.GONE
        lifecycleScope.launch {
            try {
                val r=ApiClient.get(this@LoginActivity).login(LoginRequest(email,pass))
                val body=r.body()
                if(r.isSuccessful && body!=null){
                    Prefs.saveToken(this@LoginActivity,body.token)
                    val c=ApiClient.get(this@LoginActivity).children().body()?.children?.firstOrNull()
                    if(c!=null) Prefs.saveChild(this@LoginActivity,c.id,c.name)
                    startActivity(Intent(this@LoginActivity,MainActivity::class.java)); finish()
                } else show("Giriş alınmadı. Məlumatları yoxla.")
            } catch(e:Exception){ show("Serverlə əlaqə qurulmadı: ${e.message ?: "xəta"}") }
            finally { b.loginButton.isEnabled=true; b.loginButton.text="Daxil ol" }
        }
    }
    private fun show(s:String){ b.error.text=s; b.error.visibility=View.VISIBLE }
}
