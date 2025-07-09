package br.com.local.posturadetector

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EquipeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_equipe)

        val listView = findViewById<ListView>(R.id.listViewNomes)

        //dados mais complexos (pode usar uma data class também)
        val itens = listOf(
            mapOf("titulo" to "Desenvolvedor", "subtitulo" to "Bruno Augusto"),
            mapOf("titulo" to "Embarcados e IA", "subtitulo" to "Chirs Anderson"),
            mapOf("titulo" to "Designer", "subtitulo" to "Elizabeth Carneiro"),
            mapOf("titulo" to "Documentação", "subtitulo" to "Iego Sérgio"),
            mapOf("titulo" to "Gerente de Projeto", "subtitulo" to "Margarida Nayandra"),
            mapOf("titulo" to "Supervisor da Equipe", "subtitulo" to "Vandermi João")
        )

        // Adapter personalizado
        val adapter = object : ArrayAdapter<Map<String, String>>(
            this,
            R.layout.activity_equipe,
            R.id.txtTitulo,
            itens
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent)
                val item = itens[position]

                val txtTitulo = view.findViewById<TextView>(R.id.txtTitulo)
                val txtSubtitulo = view.findViewById<TextView>(R.id.txtSubtitulo)

                txtTitulo.text = item["titulo"]
                txtSubtitulo.text = item["subtitulo"]

                return view
            }
        }

        listView.adapter = adapter
    }
}