package intro.android.goncalo_quesado

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class IntroductionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_introduction)

        // Configurar a lógica para exibir as explicações
        // Por exemplo, configurar um botão para continuar para a próxima tela
        val buttonContinue = findViewById<Button>(R.id.buttonContinue)
        buttonContinue.setOnClickListener {
            // Abrir a próxima tela após clicar no botão
            openMainActivity()
        }
    }

    private fun openMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // Finalizar esta activity para não retornar à tela de introdução ao voltar
    }
}
