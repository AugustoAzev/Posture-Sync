package br.com.local.posturadetector
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity() {
    private lateinit var textFeedback: TextView
    private lateinit var previewView: PreviewView
    private lateinit var poseDetector: PoseDetector
    private var cameraProvider: ProcessCameraProvider? = null
    private lateinit var vibrator: Vibrator
    private lateinit var mediaPlayer: MediaPlayer
    private var posturaIncorretaAnterior = false
    private var tempoInicioPosturaRuim: Long = 0
    private var tempoToleranciaMs: Long = 1500L // valor padrão
    private var caminhoAudio: Int = R.raw.alerta_postura1 // valor padrão


    override fun onCreate(savedInstanceState: Bundle?) {
        supportActionBar?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        //CORREÇÃO DA VIEW
        textFeedback = findViewById(R.id.textFeedback)
        previewView = findViewById(R.id.previewView)

        vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

        val prefs = getSharedPreferences("configuracoes", MODE_PRIVATE)
        tempoToleranciaMs = prefs.getLong("tempo_tolerancia", 1500L)
        val audioResourceName = prefs.getString("Notificação 001", "Notificação 002")
        val resId = resources.getIdentifier(audioResourceName, "raw", packageName)
        if (resId != 0) {
            caminhoAudio = resId
        }
        mediaPlayer = MediaPlayer.create(this, caminhoAudio)

        //CONFIGURAÇÃO DO DETECTOR DE POSES
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
        poseDetector = PoseDetection.getClient(options)

        //VERIFICAÇÃO DE PERMISSÕES
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun allPermissionsGranted(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            //CONFIGURAÇÃO DO PREVIEW
            val preview = Preview.Builder()
                .build()
                .also { it.setSurfaceProvider(previewView.surfaceProvider) }

            //CONFIGURAÇÃO DO ANALIZADOR DE IMAGEM
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            //SELECIONA CAMERA FROONTAL
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Falha ao vincular casos de uso", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @ExperimentalGetImage
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    val leftShoulder = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_SHOULDER)
                    val rightShoulder = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_SHOULDER)
                    val leftHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_HIP)
                    val rightHip = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_HIP)
                    val leftEar = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.LEFT_EAR)
                    val rightEar = pose.getPoseLandmark(com.google.mlkit.vision.pose.PoseLandmark.RIGHT_EAR)

                    var posturaRuimDetectada = false
                    var motivos = mutableListOf<String>()

                    //cabeça e ombros desalinhados (cabeça baseada nas orelhas)
                    if (leftShoulder != null && rightShoulder != null) {

                        val diffOmbros = Math.abs(leftShoulder.position.y - rightShoulder.position.y)
                        if (diffOmbros > 20) {
                            posturaRuimDetectada = true
                            motivos.add("Ombros desalinhados")
                        }

                        val centroOmbros = (leftShoulder.position.x + rightShoulder.position.x) / 2

                        if (leftEar != null && rightEar != null) {
                            val centroCabeca = (leftEar.position.x + rightEar.position.x) / 2
                            val deslocamentoCabeca = Math.abs(centroCabeca - centroOmbros)
                            if (deslocamentoCabeca > 30) {
                                posturaRuimDetectada = true
                                motivos.add("Cabeça fora do centro (frontal)")
                            }
                        } else if (leftEar != null) {
                            val deslocamentoEsquerda = Math.abs(leftEar.position.x - centroOmbros)
                            if (deslocamentoEsquerda > 50) {
                                posturaRuimDetectada = true
                                motivos.add("Cabeça inclinada para frente ou lado esquerdo")
                            }
                        } else if (rightEar != null) {
                            val deslocamentoDireita = Math.abs(rightEar.position.x - centroOmbros)
                            if (deslocamentoDireita > 50) {
                                posturaRuimDetectada = true
                                motivos.add("Cabeça inclinada para frente ou lado direito")
                            }
                        }
                    }

                    //coluna inclinada (centros desalinhados)
                    if (leftShoulder != null && rightShoulder != null && leftHip != null && rightHip != null) {
                        val centroOmbrosX = (leftShoulder.position.x + rightShoulder.position.x) / 2
                        val centroQuadrilX = (leftHip.position.x + rightHip.position.x) / 2
                        val desalinhamentoColuna = Math.abs(centroOmbrosX - centroQuadrilX)
                        if (desalinhamentoColuna > 30) {
                            posturaRuimDetectada = true
                            motivos.add("Coluna inclinada")
                        }
                    }

                    val currentTime = System.currentTimeMillis()

                    if (posturaRuimDetectada) {
                        if (!posturaIncorretaAnterior) {
                            posturaIncorretaAnterior = true
                            tempoInicioPosturaRuim = currentTime
                        } else if (currentTime - tempoInicioPosturaRuim >= tempoToleranciaMs) {
                            val mensagem = "Postura incorreta: ${motivos.joinToString(", ")}"
                            textFeedback.text = mensagem
                            textFeedback.setTextColor(Color.RED)

                            if (!mediaPlayer.isPlaying) mediaPlayer.start()
                            vibrator.vibrate(
                                VibrationEffect.createOneShot(
                                    100,
                                    VibrationEffect.DEFAULT_AMPLITUDE
                                )
                            )
                            Log.d(TAG, mensagem)
                        }
                    } else {
                        textFeedback.text = "Postura correta"
                        textFeedback.setTextColor(Color.GREEN)
                        posturaIncorretaAnterior = false
                        tempoInicioPosturaRuim = 0
                        Log.d(TAG, "Postura correta.")
                    }
                }
                .addOnFailureListener {
                    Log.e(TAG, "Erro ao detectar pose", it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(this, "Permissões não concedidas", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        poseDetector.close()
    }

    companion object {
        private const val TAG = "CameraActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}