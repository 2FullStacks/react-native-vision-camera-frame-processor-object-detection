package com.visioncamerarealtimeobjectdetection.realtimeobjectdetectionprocessor

import com.facebook.react.bridge.ReactApplicationContext

import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.components.containers.Detection
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector

import com.mrousavy.camera.frameprocessors.Frame
import com.mrousavy.camera.frameprocessors.VisionCameraProxy
import com.mrousavy.camera.frameprocessors.FrameProcessorPlugin

class RealtimeObjectDetectionProcessorPlugin(proxy: VisionCameraProxy, options: Map<String, Any>?): FrameProcessorPlugin() {
    private val _context: ReactApplicationContext = proxy.context
    private var _detector: ObjectDetector? = null

    fun getDetectorWithModelFile(config: Map<String, Any>): ObjectDetector {
        if (_detector == null) {
            val modelFile = config["modelFile"].toString()

            val maxResults = (config["maxResults"] as? Number)?.toInt()
            val scoreThreshold = (config["scoreThreshold"] as? Number)?.toFloat()

            val optionsBuilder = ObjectDetector.ObjectDetectorOptions.builder()
                .setBaseOptions(BaseOptions.builder().setModelAssetPath(modelFile).build())
                .setRunningMode(RunningMode.IMAGE)
                .setMaxResults(maxResults)

            if (scoreThreshold != null && scoreThreshold > 0) {
                optionsBuilder.setScoreThreshold(scoreThreshold)
            }

            val options = optionsBuilder.build()

            _detector = ObjectDetector.createFromOptions(_context, options);
        }
        return _detector!!
    }

    fun convertToConfigWithDefault(input: Map<String, Any>?): Map<String, Any> {
        return input ?: emptyMap()
    }

    override fun callback(frame: Frame, arguments: Map<String, Any>?): Any? {
        val mediaImage = frame.image

        val results: MutableList<Any> = arrayListOf()

        if (mediaImage == null) {
            return results
        }

        val config = convertToConfigWithDefault(arguments)

        val bitmap = frame.getImageProxy().toBitmap()
        val mlImage = BitmapImageBuilder(bitmap).build()

        val detectedObjects = getDetectorWithModelFile(config).detect(mlImage)?.detections()

        detectedObjects?.forEach { detectedObject ->
            val labels: MutableList<Any> = arrayListOf()

            detectedObject.categories().forEach { label ->
                labels.add(mapOf(
                    "index" to label.index(),
                    "label" to label.categoryName(),
                    "confidence" to label.score().toDouble()
                ))
            }

            if (labels.isNotEmpty()) {
                results.add(mapOf(
                    "labels" to labels,
                    "top" to detectedObject.boundingBox().top.toDouble(),
                    "left" to detectedObject.boundingBox().left.toDouble(),
                    "width" to detectedObject.boundingBox().width().toDouble(),
                    "height" to detectedObject.boundingBox().height().toDouble()
                ))
            }
        }

        return results
    }

}
