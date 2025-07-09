package br.com.local.posturadetector

import android.content.Context
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ConfiguracoesActivity : AppCompatActivity() {

    private lateinit var seekBar: SeekBar
    private lateinit var tempoTexto: TextView
    private lateinit var spinnerAudio: Spinner
    private val audiosDisponiveis = listOf("Notificação 001", "Notificação 002")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracoes)

        seekBar = findViewById(R.id.seekBarTempo)
        tempoTexto = findViewById(R.id.textTempo)
        spinnerAudio = findViewById(R.id.spinnerAudio)

        val prefs = getSharedPreferences("configuracoes", Context.MODE_PRIVATE)

        //tempo atual salvo
        val tempoAtual = prefs.getLong("tempo_tolerancia", 1500L)
        seekBar.progress = (tempoAtual / 100).toInt()
        tempoTexto.text = "Tempo: ${tempoAtual}ms"

        seekBar.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val tempoMs = progress * 100L
                tempoTexto.text = "Tempo: ${tempoMs}ms"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                val tempoMs = seekBar?.progress?.times(100L) ?: 1500L
                prefs.edit().putLong("tempo_tolerancia", tempoMs).apply()
            }
        })

        //spinner de áudios
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, audiosDisponiveis)
        spinnerAudio.adapter = adapter

        val audioAtual = prefs.getString("Notificação 002", "Notificação 001")
        spinnerAudio.setSelection(audiosDisponiveis.indexOf(audioAtual))

        spinnerAudio.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val audioSelecionado = audiosDisponiveis[position]
                prefs.edit().putString("audio_nome", audioSelecionado).apply()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}
