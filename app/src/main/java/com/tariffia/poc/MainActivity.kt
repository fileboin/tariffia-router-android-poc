package com.tariffia.poc

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var log: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        status = findViewById(R.id.txtStatus)
        log = findViewById(R.id.txtLog)

        findViewById<Button>(R.id.btnStart).setOnClickListener {
            append("> START")
            io.execute {
                val msg = runCatching { RouterController.start(this) }
                    .getOrElse { "start failed: ${it.message}" }
                ui { append(msg) }
            }
        }

        findViewById<Button>(R.id.btnStop).setOnClickListener {
            append("> STOP")
            RouterController.stop()
        }

        findViewById<Button>(R.id.btnHealthz).setOnClickListener { call("GET /healthz") { RouterController.get("/healthz") } }
        findViewById<Button>(R.id.btnModels).setOnClickListener { call("GET /v1/models") { RouterController.get("/v1/models") } }
        findViewById<Button>(R.id.btnMessages).setOnClickListener { call("POST /v1/messages") { RouterController.postMessages() } }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { refreshInfo() }

        append("runtime libnode = ${runCatching { RouterController.nativeNodeVersion() }.getOrElse { "?" }}")
        refreshInfo()
    }

    private fun call(label: String, block: () -> String) {
        append("> $label")
        io.execute {
            val r = runCatching { block() }.getOrElse { "error: ${it.message}" }
            ui { append(r) }
        }
    }

    private fun refreshInfo() {
        io.execute {
            val info = RouterController.readInfo(this)
            val text = if (info == null) {
                "node-info.json: not written yet (start Router first)"
            } else {
                "node ${info.optString("version")} arch=${info.optString("arch")} platform=${info.optString("platform")}"
            }
            ui {
                status.text = "status: $text"
                append(text)
            }
        }
    }

    private fun ui(block: () -> Unit) = runOnUiThread(block)

    private fun append(line: String) {
        log.append(if (log.text.isEmpty()) line else "\n$line")
    }

    override fun onDestroy() {
        io.shutdownNow()
        super.onDestroy()
    }
}
