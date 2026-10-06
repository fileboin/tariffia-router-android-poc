package com.tariffia.poc

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

/**
 * PoC-only controller. It extracts the bundled, unmodified Tariffia Router `dist`
 * into the app files dir, starts the embedded Node 24 runtime through the JNI
 * bridge, and issues a few loopback HTTP calls.
 *
 * No provider keys, no production lifecycle, no foreground service.
 */
object RouterController {

    const val HOST = "127.0.0.1"
    const val PORT = 8910

    // Temporary, local-only test token for the PoC. Never a real provider key.
    const val TEST_TOKEN = "poc-local-test-token-0000000000000000"

    private const val ROUTER_DIR_NAME = "router"
    private const val DIST_ZIP_ASSET = "router-dist.zip"
    private const val LAUNCHER_ASSET = "poc-launcher.mjs"

    @Volatile
    private var started = false

    init {
        // libnode.so must be resolvable before the bridge that depends on it.
        System.loadLibrary("node")
        System.loadLibrary("nodepoc")
    }

    private external fun nativeStart(
        workDir: String,
        scriptPath: String,
        stdoutPath: String,
        stderrPath: String,
        envPairs: Array<String>,
        nodeArgs: Array<String>
    ): Boolean

    external fun nativeNodeVersion(): String

    private fun routerDir(ctx: Context): File = File(ctx.filesDir, ROUTER_DIR_NAME)

    fun infoFile(ctx: Context): File = File(ctx.filesDir, "node-info.json")

    /** Extract the unmodified Router dist bundle (dist/ + registry/) on first use. */
    private fun ensureRuntime(ctx: Context): File {
        val root = routerDir(ctx)
        val marker = File(root, "dist/src/cli/index.js")
        if (!marker.exists()) {
            root.mkdirs()
            ctx.assets.open(DIST_ZIP_ASSET).use { input ->
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        val out = File(root, entry.name)
                        // Defensive: keep every extracted path inside root.
                        if (!out.canonicalPath.startsWith(root.canonicalPath + File.separator)) {
                            throw SecurityException("zip entry escapes root: ${entry.name}")
                        }
                        if (entry.isDirectory) {
                            out.mkdirs()
                        } else {
                            out.parentFile?.mkdirs()
                            FileOutputStream(out).use { fos -> zip.copyTo(fos) }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
        }
        // Copy the PoC launcher next to the router tree.
        val launcher = File(root, LAUNCHER_ASSET)
        if (!launcher.exists()) {
            ctx.assets.open(LAUNCHER_ASSET).use { input ->
                FileOutputStream(launcher).use { fos -> input.copyTo(fos) }
            }
        }
        return root
    }

    fun start(ctx: Context): String {
        if (started) return "already started"
        val root = ensureRuntime(ctx)

        val launcher = File(root, LAUNCHER_ASSET).absolutePath
        val entry = File(root, "dist/src/cli/index.js").absolutePath
        val registry = File(root, "registry/ollama.json").absolutePath
        val stdout = File(root, "node.out").absolutePath
        val stderr = File(root, "node.err").absolutePath

        val env = arrayOf(
            "TARIFFIA_HOST", HOST,
            "TARIFFIA_PORT", PORT.toString(),
            "TARIFFIA_TOKEN", TEST_TOKEN,
            "TARIFFIA_MODE", "FREE_ONLY",
            "TARIFFIA_REGISTRY", registry,
            "TMPDIR", ctx.cacheDir.absolutePath,
            "HOME", ctx.filesDir.absolutePath,
            "NODE_OPTIONS", "--max-old-space-size-percentage=50",
            "NODE_COMPILE_CACHE", File(ctx.cacheDir, "ncc").absolutePath,
            "NODE_COMPILE_CACHE_PORTABLE", "1",
            "POC_INFO_FILE", infoFile(ctx).absolutePath,
            "POC_ROUTER_DIR", root.absolutePath,
            "POC_ROUTER_ENTRY", entry
        )

        val ok = nativeStart(root.absolutePath, launcher, stdout, stderr, env, arrayOf(launcher))
        if (!ok) return "nativeStart returned false"
        started = true
        return "node::Start launched (pid ${android.os.Process.myPid()})"
    }

    fun stop(): String {
        // Embedded Node has no graceful stop API; the PoC kills the app process.
        android.os.Process.killProcess(android.os.Process.myPid())
        return "killing process"
    }

    fun readInfo(ctx: Context): JSONObject? {
        val f = infoFile(ctx)
        if (!f.exists()) return null
        return runCatching { JSONObject(f.readText()) }.getOrNull()
    }

    fun tailLog(ctx: Context, lines: Int = 40): String {
        val f = File(routerDir(ctx), "node.out")
        if (!f.exists()) return "(no node.out yet)"
        val all = f.readLines()
        return all.takeLast(lines).joinToString("\n")
    }

    fun tailErr(ctx: Context, lines: Int = 20): String {
        val f = File(routerDir(ctx), "node.err")
        if (!f.exists()) return ""
        return f.readLines().takeLast(lines).joinToString("\n")
    }

    private fun open(method: String, path: String): HttpURLConnection {
        val conn = URL("http://$HOST:$PORT$path").openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 4000
        conn.readTimeout = 8000
        conn.setRequestProperty("Authorization", "Bearer $TEST_TOKEN")
        return conn
    }

    fun get(path: String): String {
        val conn = open("GET", path)
        return try {
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            "HTTP $code\n${body.take(4000)}"
        } catch (e: Exception) {
            "request failed: ${e.message}"
        } finally {
            conn.disconnect()
        }
    }

    fun postMessages(): String {
        val conn = open("POST", "/v1/messages")
        conn.doOutput = true
        conn.setRequestProperty("Content-Type", "application/json")
        // Deliberately uses a non-existent model: the goal is to prove the route
        // is reachable and does not crash on ICU/Node runtime errors. No real key.
        val payload = """{"model":"poc-nonexistent-model","max_tokens":16,"messages":[{"role":"user","content":"ping"}]}"""
        return try {
            conn.outputStream.use { it.write(payload.toByteArray()) }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            "HTTP $code\n${body.take(4000)}"
        } catch (e: Exception) {
            "request failed: ${e.message}"
        } finally {
            conn.disconnect()
        }
    }
}
