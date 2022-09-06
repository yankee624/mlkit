/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import androidx.annotation.Dimension;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.common.model.LocalModel;
import com.google.mlkit.vision.demo.CameraSource;
import com.google.mlkit.vision.demo.CameraSourcePreview;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;
import com.google.mlkit.vision.demo.java.facedetector.FaceDetectorProcessor;
import com.google.mlkit.vision.demo.java.labeldetector.LabelDetectorProcessor;
import com.google.mlkit.vision.demo.java.objectdetector.ObjectDetectorProcessor;
import com.google.mlkit.vision.demo.java.posedetector.PoseDetectorProcessor;
import com.google.mlkit.vision.demo.java.segmenter.SegmenterProcessor;
import com.google.mlkit.vision.demo.java.textdetector.TextRecognitionProcessor;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.demo.preference.SettingsActivity;
import com.google.mlkit.vision.label.custom.CustomImageLabelerOptions;
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions;
import com.google.mlkit.vision.objects.custom.CustomObjectDetectorOptions;
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions;
import com.google.mlkit.vision.pose.PoseDetectorOptionsBase;
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions;
import com.google.mlkit.vision.text.devanagari.DevanagariTextRecognizerOptions;
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

/** Live preview demo for ML Kit APIs. */
@KeepName
public final class LivePreviewActivity extends AppCompatActivity
    implements OnItemSelectedListener, CompoundButton.OnCheckedChangeListener {
  private static final String OBJECT_DETECTION = "Object Detection";
  private static final String OBJECT_DETECTION_CUSTOM = "Custom Object Detection";
  private static final String CUSTOM_AUTOML_OBJECT_DETECTION =
      "Custom AutoML Object Detection (Flower)";
  private static final String FACE_DETECTION = "Face Detection";
  private static final String BARCODE_SCANNING = "Barcode Scanning";
  private static final String IMAGE_LABELING = "Image Labeling";
  private static final String IMAGE_LABELING_CUSTOM = "Custom Image Labeling (Birds)";
  private static final String CUSTOM_AUTOML_LABELING = "Custom AutoML Image Labeling (Flower)";
  private static final String POSE_DETECTION = "Pose Detection";
  private static final String SELFIE_SEGMENTATION = "Selfie Segmentation";
  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";
  private static final String TEXT_RECOGNITION_CHINESE = "Text Recognition Chinese (Beta)";
  private static final String TEXT_RECOGNITION_DEVANAGARI = "Text Recognition Devanagari (Beta)";
  private static final String TEXT_RECOGNITION_JAPANESE = "Text Recognition Japanese (Beta)";
  private static final String TEXT_RECOGNITION_KOREAN = "Text Recognition Korean (Beta)";

  private static final String TAG = "LivePreviewActivity";

  private CameraSource cameraSource = null;
  private CameraSourcePreview preview;
  private GraphicOverlay graphicOverlay;
  private String selectedModel = POSE_DETECTION;


  final int NUM_STAGE = 3;
  int curr_stage = 0;
  Drawable progressBarTemplateDrawable;
  ImageView[] progressBarEdges = new ImageView[NUM_STAGE];
  ImageView[] progressBarTemplates = new ImageView[NUM_STAGE];
  ImageView[] progressBars = new ImageView[NUM_STAGE];
  TextView[] stageTexts = new TextView[NUM_STAGE];
  TextView[] scoreTexts = new TextView[NUM_STAGE];
  TextView hitMissText;
  TextView finalResultText;

  boolean SHOW_PAST_PROGRESS = true;
  boolean DRAW_JOINTS = true;

  long ANALYZE_STAGE_TIME = 2000;
  long STAGE_DURATION = 5000;
  long TOTAL_PLAY_TIME = NUM_STAGE*STAGE_DURATION + ANALYZE_STAGE_TIME;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate");

    setContentView(R.layout.activity_vision_live_preview);

    preview = findViewById(R.id.preview_view);
    if (preview == null) {
      Log.d(TAG, "Preview is null");
    }
    graphicOverlay = findViewById(R.id.graphic_overlay);
    if (graphicOverlay == null) {
      Log.d(TAG, "graphicOverlay is null");
    }

    Spinner spinner = findViewById(R.id.spinner);
    List<String> options = new ArrayList<>();
    options.add(POSE_DETECTION);
    options.add(OBJECT_DETECTION);


    // Creating adapter for spinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, R.layout.spinner_style, options);
    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    // attaching data adapter to spinner
    spinner.setAdapter(dataAdapter);
    spinner.setOnItemSelectedListener(this);

    ToggleButton facingSwitch = findViewById(R.id.facing_switch);
    facingSwitch.setOnCheckedChangeListener(this);

    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
        v -> {
          Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
          intent.putExtra(
              SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.LIVE_PREVIEW);
          startActivity(intent);
        });

    createCameraSource(selectedModel);
    hitMissText = findViewById(R.id.hitMissText);
    finalResultText = findViewById(R.id.finalResultText);
    stageTexts[0] = findViewById(R.id.stageTextView1);
    stageTexts[1] = findViewById(R.id.stageTextView2);
    stageTexts[2] = findViewById(R.id.stageTextView3);
    scoreTexts[0] = findViewById(R.id.scoreTextView1);
    scoreTexts[1] = findViewById(R.id.scoreTextView2);
    scoreTexts[2] = findViewById(R.id.scoreTextView3);
    progressBarTemplates[0] = findViewById(R.id.progressBarTemplate1);
    progressBarTemplates[1] = findViewById(R.id.progressBarTemplate2);
    progressBarTemplates[2] = findViewById(R.id.progressBarTemplate3);
    progressBarEdges[0] = findViewById(R.id.progressBarEdge1);
    progressBarEdges[1] = findViewById(R.id.progressBarEdge2);
    progressBarEdges[2] = findViewById(R.id.progressBarEdge3);
    progressBars[0] = findViewById(R.id.progressBar1);
    progressBars[1] = findViewById(R.id.progressBar2);
    progressBars[2] = findViewById(R.id.progressBar3);
    for (int i = 0; i < progressBars.length; i++) {
      progressBarTemplates[i].getLayoutParams().height = 150;
      progressBarEdges[i].getLayoutParams().height = 150;
      progressBars[i].getLayoutParams().height = 150;
    }
    for (TextView stageText: stageTexts) {
      stageText.setTextSize(Dimension.SP, 20);
      stageText.setTextColor(Color.parseColor("#E54A4A"));
    }
    for (TextView scoreText: scoreTexts) {
      scoreText.setTextSize(Dimension.SP, 20);
      scoreText.setTextColor(Color.parseColor("#332F2F"));
    }
    finalResultText.setTextSize(Dimension.SP, 40);
    finalResultText.setTextColor(Color.parseColor("#23FF00"));
    hitMissText.setTextSize(Dimension.SP, 40);

    progressBarTemplateDrawable = getDrawable(R.drawable.progress_bar_template);


    if (!SHOW_PAST_PROGRESS) {
      for (ImageView progressBarTemplate: progressBarTemplates) {
        progressBarTemplate.setVisibility(View.INVISIBLE);
      }
      for (TextView stageText: stageTexts) {
        stageText.setVisibility(View.INVISIBLE);
      }
    }

    Timer timer = new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        if (SHOW_PAST_PROGRESS) {
          showHitMiss();
          progressAnimation();
          showFinalScore();
        } else {
          showHitMiss();
          try {
            Thread.sleep(NUM_STAGE * STAGE_DURATION);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          curr_stage = NUM_STAGE;
          showLoading();
          try {
            Thread.sleep(ANALYZE_STAGE_TIME);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          showFinalScore();
        }
      }
    }, 10000);
  }

  public void showHitMiss() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Random random = new Random();
        while (curr_stage < NUM_STAGE) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              if (random.nextInt(100) < 70) {
                hitMissText.setText("Hit!");
                hitMissText.setTextColor(Color.parseColor("#00FF59"));
              } else {
                hitMissText.setText("Miss!");
                hitMissText.setTextColor(Color.parseColor("#F92C10"));
              }
            }
          });
          try {
            Thread.sleep(1000);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    }).start();
  }

  public void showLoading() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        hitMissText.setText("");
        finalResultText.setText("Loading...");
      }
    });
  }

  public void showFinalScore() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        finalResultText.setText("Great job!\nTotal score: 85");
      }
    });
  }

  public void markCurrentProgress(int stage) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        // All stage done: Turn off all stageText colors & progress bar edges
        if (stage >= NUM_STAGE) {
          for (int i = 0; i < NUM_STAGE; i++) {
            stageTexts[i].setTextColor(Color.parseColor("#D5D5D5"));
          }

          for (ImageView progressBarEdge: progressBarEdges) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                progressBarEdge.setVisibility(View.INVISIBLE);
              }
            });
          }
          return;
        }

        for (int i = 0; i < NUM_STAGE; i++) {
          if (i == stage) stageTexts[i].setTextColor(Color.parseColor("#E54A4A"));
          else  stageTexts[i].setTextColor(Color.parseColor("#D5D5D5"));
        }


        // Blinking: visible -> invisible -> visible -> ...
        while (curr_stage == stage) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              progressBarEdges[stage].setVisibility(View.VISIBLE);
            }
          });

          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              progressBarEdges[stage].setVisibility(View.INVISIBLE);
            }
          });
          try {
            Thread.sleep(500);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }


      }
    }).start();

  }

  public void progressAnimation() {
    long start = System.currentTimeMillis();
    for (int i = 0; i < NUM_STAGE+1; i++) {
      curr_stage = i;
      markCurrentProgress(i);


      // Show progress of last stage
      if (i >= 1) {
        if (i == NUM_STAGE) showLoading();
        fillProgressBar(progressBars[i-1], progressBarTemplates[i-1], scoreTexts[i-1]);
      }

      if (i == NUM_STAGE) break;

      // Wait until current stage ends
      try {
        Thread.sleep(i == 0 ? STAGE_DURATION : STAGE_DURATION - ANALYZE_STAGE_TIME);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    Log.e(TAG, "progress animation total: "+(System.currentTimeMillis()-start));
  }

  public void fillProgressBar(ImageView progressBar, ImageView progressBarTemplate, TextView scoreText) {
    int templateWidth = progressBarTemplate.getWidth();
    Log.e(TAG, "target width: "+templateWidth);

    int steps = 20;
    for (int step = 0; step < steps; step++) {
      final int currWidth;
      if (step == steps-1) {
        currWidth = templateWidth;
      } else {
        currWidth = (int) (templateWidth * step / (double) steps);
      }

      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          progressBar.getLayoutParams().width = currWidth;
          progressBar.requestLayout();
        }
      });

      try {
        Thread.sleep(ANALYZE_STAGE_TIME/steps);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        scoreText.setVisibility(View.VISIBLE);
      }
    });

  }

  @Override
  public synchronized void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
    // An item was selected. You can retrieve the selected item using
    // parent.getItemAtPosition(pos)
    selectedModel = parent.getItemAtPosition(pos).toString();
    Log.d(TAG, "Selected model: " + selectedModel);
    preview.stop();
    createCameraSource(selectedModel);
    startCameraSource();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    Log.d(TAG, "Set facing");
    if (cameraSource != null) {
      if (isChecked) {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
      } else {
        cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
      }
    }
    preview.stop();
    startCameraSource();
  }

  private void createCameraSource(String model) {
    // If there's no existing cameraSource, create one.
    if (cameraSource == null) {
      cameraSource = new CameraSource(this, graphicOverlay);
      cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
    }

    try {
      switch (model) {
        case OBJECT_DETECTION:
          Log.i(TAG, "Using Object Detector Processor");
          ObjectDetectorOptions objectDetectorOptions =
              PreferenceUtils.getObjectDetectorOptionsForLivePreview(this);
          cameraSource.setMachineLearningFrameProcessor(
              new ObjectDetectorProcessor(this, objectDetectorOptions));
          break;
        case OBJECT_DETECTION_CUSTOM:
          Log.i(TAG, "Using Custom Object Detector Processor");
          LocalModel localModel =
              new LocalModel.Builder()
                  .setAssetFilePath("custom_models/object_labeler.tflite")
                  .build();
          CustomObjectDetectorOptions customObjectDetectorOptions =
              PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(this, localModel);
          cameraSource.setMachineLearningFrameProcessor(
              new ObjectDetectorProcessor(this, customObjectDetectorOptions));
          break;
        case CUSTOM_AUTOML_OBJECT_DETECTION:
          Log.i(TAG, "Using Custom AutoML Object Detector Processor");
          LocalModel customAutoMLODTLocalModel =
              new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
          CustomObjectDetectorOptions customAutoMLODTOptions =
              PreferenceUtils.getCustomObjectDetectorOptionsForLivePreview(
                  this, customAutoMLODTLocalModel);
          cameraSource.setMachineLearningFrameProcessor(
              new ObjectDetectorProcessor(this, customAutoMLODTOptions));
          break;
        case TEXT_RECOGNITION_LATIN:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build()));
          break;
        case TEXT_RECOGNITION_CHINESE:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Chinese.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(
                  this, new ChineseTextRecognizerOptions.Builder().build()));
          break;
        case TEXT_RECOGNITION_DEVANAGARI:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Devanagari.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(
                  this, new DevanagariTextRecognizerOptions.Builder().build()));
          break;
        case TEXT_RECOGNITION_JAPANESE:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Japanese.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(
                  this, new JapaneseTextRecognizerOptions.Builder().build()));
          break;
        case TEXT_RECOGNITION_KOREAN:
          Log.i(TAG, "Using on-device Text recognition Processor for Latin and Korean.");
          cameraSource.setMachineLearningFrameProcessor(
              new TextRecognitionProcessor(
                  this, new KoreanTextRecognizerOptions.Builder().build()));
          break;
        case FACE_DETECTION:
          Log.i(TAG, "Using Face Detector Processor");
          cameraSource.setMachineLearningFrameProcessor(new FaceDetectorProcessor(this));
          break;
        case BARCODE_SCANNING:
          Log.i(TAG, "Using Barcode Detector Processor");
          cameraSource.setMachineLearningFrameProcessor(new BarcodeScannerProcessor(this));
          break;
        case IMAGE_LABELING:
          Log.i(TAG, "Using Image Label Detector Processor");
          cameraSource.setMachineLearningFrameProcessor(
              new LabelDetectorProcessor(this, ImageLabelerOptions.DEFAULT_OPTIONS));
          break;
        case IMAGE_LABELING_CUSTOM:
          Log.i(TAG, "Using Custom Image Label Detector Processor");
          LocalModel localClassifier =
              new LocalModel.Builder()
                  .setAssetFilePath("custom_models/bird_classifier.tflite")
                  .build();
          CustomImageLabelerOptions customImageLabelerOptions =
              new CustomImageLabelerOptions.Builder(localClassifier).build();
          cameraSource.setMachineLearningFrameProcessor(
              new LabelDetectorProcessor(this, customImageLabelerOptions));
          break;
        case CUSTOM_AUTOML_LABELING:
          Log.i(TAG, "Using Custom AutoML Image Label Detector Processor");
          LocalModel customAutoMLLabelLocalModel =
              new LocalModel.Builder().setAssetManifestFilePath("automl/manifest.json").build();
          CustomImageLabelerOptions customAutoMLLabelOptions =
              new CustomImageLabelerOptions.Builder(customAutoMLLabelLocalModel)
                  .setConfidenceThreshold(0)
                  .build();
          cameraSource.setMachineLearningFrameProcessor(
              new LabelDetectorProcessor(this, customAutoMLLabelOptions));
          break;
        case POSE_DETECTION:
          PoseDetectorOptionsBase poseDetectorOptions =
              PreferenceUtils.getPoseDetectorOptionsForLivePreview(this);
          Log.i(TAG, "Using Pose Detector with options " + poseDetectorOptions);
          boolean shouldShowInFrameLikelihood =
              PreferenceUtils.shouldShowPoseDetectionInFrameLikelihoodLivePreview(this);
          boolean visualizeZ = PreferenceUtils.shouldPoseDetectionVisualizeZ(this);
          boolean rescaleZ = PreferenceUtils.shouldPoseDetectionRescaleZForVisualization(this);
          boolean runClassification = PreferenceUtils.shouldPoseDetectionRunClassification(this);
          cameraSource.setMachineLearningFrameProcessor(
              new PoseDetectorProcessor(
                  this,
                  poseDetectorOptions,
                  shouldShowInFrameLikelihood,
                  visualizeZ,
                  rescaleZ,
                  runClassification,
                  /* isStreamMode = */ true,
                      DRAW_JOINTS));
          break;
        case SELFIE_SEGMENTATION:
          cameraSource.setMachineLearningFrameProcessor(new SegmenterProcessor(this));
          break;
        default:
          Log.e(TAG, "Unknown model: " + model);
      }
    } catch (RuntimeException e) {
      Log.e(TAG, "Can not create image processor: " + model, e);
      Toast.makeText(
              getApplicationContext(),
              "Can not create image processor: " + e.getMessage(),
              Toast.LENGTH_LONG)
          .show();
    }
  }

  /**
   * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
   * (e.g., because onResume was called before the camera source was created), this will be called
   * again when the camera source is created.
   */
  private void startCameraSource() {
    if (cameraSource != null) {
      try {
        if (preview == null) {
          Log.d(TAG, "resume: Preview is null");
        }
        if (graphicOverlay == null) {
          Log.d(TAG, "resume: graphOverlay is null");
        }
        preview.start(cameraSource, graphicOverlay);
      } catch (IOException e) {
        Log.e(TAG, "Unable to start camera source.", e);
        cameraSource.release();
        cameraSource = null;
      }
    }

  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createCameraSource(selectedModel);
    startCameraSource();
  }

  /** Stops the camera. */
  @Override
  protected void onPause() {
    super.onPause();
    preview.stop();
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (cameraSource != null) {
      cameraSource.release();
    }
  }
}
