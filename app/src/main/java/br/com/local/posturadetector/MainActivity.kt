package br.com.local.posturadetector

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnStartCamera = findViewById<LinearLayout>(R.id.btnStartCamera)
        val btnListaNomes = findViewById<LinearLayout>(R.id.btnEquipe)
        val btnConfiguracoes = findViewById<LinearLayout>(R.id.btnConfiguracoes)
        val btnSobre = findViewById<LinearLayout>(R.id.btnSobre)

        btnStartCamera.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            startActivity(intent)
        }

        btnListaNomes.setOnClickListener {
            val intent = Intent(this, EquipeActivity::class.java)
            startActivity(intent)
        }

        btnConfiguracoes.setOnClickListener {
            val intent = Intent(this, ConfiguracoesActivity::class.java)
            startActivity(intent)
        }

        btnSobre.setOnClickListener {
            val intent = Intent(this, SobreActivity::class.java)
            startActivity(intent)
        }
    }
}