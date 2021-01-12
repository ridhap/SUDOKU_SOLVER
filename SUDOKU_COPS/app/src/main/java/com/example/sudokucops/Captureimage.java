package com.example.sudokucops;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Space;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Captureimage extends AppCompatActivity {
    StorageReference storageReference;
    String url;
    public static int[][] sudokuGrid;
    //contains reference to the sudoku cells
    private static EditText[][] gridCell;
    //contains value of integer in cells, if blank then 0
    public int[][] cellValues = new int[9][9];
    public int[][] test_sudo = new int[9][9];
    public int[][] result = new int[9][9];
    private static int cellDimensions;
    Bitmap bitmap, graybitmap;
    ImageView viewImage;
    Button b, Process;
    Intent intent;
    Uri uri;
    Mat cropped;


    public static final int RequestPermissionCode = 1;

    //Dialog
    ProgressDialog pd;
    // Used for logging success or failure messages
    private static final String TAG = "OCVSample::Activity";
    TessBaseAPI tessBaseApi;

    private static final String lang = "eng";
    // String DATA_PATH = "gs://trial-a6371.appspot.com/Images/Text";
    //String DATA_PATH="https://firebasestorage.googleapis.com/v0/b/sudokucop.appspot.com/o/Images%2FText?alt=media&token=c90f84c5-d79a-416c-8fc0-5626a65c8681" ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_captureimage);

        storageReference = FirebaseStorage.getInstance().getReference("Images");
        GridLayout gridLayout = findViewById(R.id.sudokuGrid);
        //get screen size in pixels to adjust size of sudoku cells
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            display.getSize(size);
        }
        int dimensions = size.x / 11;
        SudokuGrid.initGrid(this, gridLayout, dimensions);

        final Button solveButton = findViewById(R.id.solveButton);
        solveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SudokuGrid.getCellValues();
                if (!SudokuGrid.getSolution()) {
                    Toast.makeText(getApplicationContext(), "Solution does not exist", Toast.LENGTH_SHORT).show();
                    return;
                }
                Toast.makeText(getApplicationContext(), "Solution Found", Toast.LENGTH_SHORT).show();
                SudokuGrid.updateSolution(result);

            }
        });
        Button clearButton = findViewById(R.id.clearButton);
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SudokuGrid.clearGrid();
            }
        });

        EnableRuntimePermission();
        if (OpenCVLoader.initDebug()) {
//            System.out.println("OpenCV loaded successfully");
            Toast.makeText(getApplicationContext(), "OpenCV loaded successfully", Toast.LENGTH_SHORT).show();
        } else {
//            System.out.println("OpenCV not loaded successfully");
            Toast.makeText(getApplicationContext(), "OpenCV not loaded successfully", Toast.LENGTH_SHORT).show();
        }


        b = (Button) findViewById(R.id.btnSelectPhoto);
        viewImage = (ImageView) findViewById(R.id.viewImage);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectImage();
            }
        });
        Process = findViewById(R.id.process);
        Process.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    process();

            }
        });
    }

    private void selectImage() {
        final CharSequence[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        AlertDialog.Builder builder = new AlertDialog.Builder(Captureimage.this);
        builder.setTitle("Add Photo!");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (options[item].equals("Take Photo")) {
                    intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                    startActivityForResult(intent, 7);
                } else if (options[item].equals("Choose from Gallery")) {
                    intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");

                    startActivityForResult(intent, 2);
                } else if (options[item].equals("Cancel")) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == 7) {
                bitmap = (Bitmap) data.getExtras().get("data");
                viewImage.setImageBitmap(bitmap);

                final ProgressDialog progressDialog = new ProgressDialog(Captureimage.this);
                progressDialog.setMessage("on process");
                progressDialog.create();
                progressDialog.show();
                // Get the data from an ImageView as bytes
                viewImage.setDrawingCacheEnabled(true);
                viewImage.buildDrawingCache();
                Bitmap bitmap = ((BitmapDrawable) viewImage.getDrawable()).getBitmap();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
                byte[] datas = baos.toByteArray();

                UploadTask uploadTask = storageReference.child("Text").putBytes(datas);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(Captureimage.this, "Failed " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        progressDialog.dismiss();
                        Toast.makeText(Captureimage.this, "Image Uploaded!!", Toast.LENGTH_SHORT).show();
                    }
                });
                viewImage.setImageBitmap(bitmap);

            } else if (requestCode == 2) {

                Uri uri = data.getData();

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                    final ProgressDialog progressDialog = new ProgressDialog(Captureimage.this);
                    progressDialog.setMessage("on process");
                    progressDialog.create();
                    progressDialog.show();
                    storageReference.child("Text").putFile(data.getData()).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            storageReference.child("Text").putFile(data.getData());
                            if (task.isSuccessful()) {

                                storageReference.child("Text").getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {

                                        url = task.getResult().toString();
//                                        Picasso.get().load(url).into(viewImage);

                                    }
                                });


                                Toast.makeText(Captureimage.this, "Image Uploaded", Toast.LENGTH_SHORT).show();
                                Captureimage.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        progressDialog.dismiss();
                                    }
                                });
                            } else {
                                Toast.makeText(Captureimage.this, "Try Again", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    viewImage.setImageBitmap(bitmap);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void EnableRuntimePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(Captureimage.this,
                Manifest.permission.CAMERA)) {

            Toast.makeText(Captureimage.this, "CAMERA permission allows us to Access CAMERA app", Toast.LENGTH_LONG).show();

        } else {
            ActivityCompat.requestPermissions(Captureimage.this, new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);

        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(Captureimage.this, "Permission Granted, Now your application can access CAMERA.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(Captureimage.this, "Permission Canceled, Now your application cannot access CAMERA.", Toast.LENGTH_LONG).show();

                }
                break;
        }

    }

    protected void process(){
        Mat Rgba = new Mat();
        Mat greymat = new Mat();
        Mat sudoku = new Mat();
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inDither = false;
        o.inSampleSize = 4;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        graybitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        //bit map to mat
        Utils.bitmapToMat(bitmap, Rgba);
        Imgproc.cvtColor(Rgba, greymat, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(Rgba, sudoku, Imgproc.COLOR_RGB2GRAY);
        Utils.matToBitmap(greymat, graybitmap);


        Mat blurMat = new Mat();
        Imgproc.GaussianBlur(greymat, blurMat, new Size(5, 5), 0);
        Mat thresh = new Mat();
        Imgproc.adaptiveThreshold(blurMat, thresh, 255, 1, 1, 11, 2);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hier = new Mat();
        Imgproc.findContours(thresh, contours, hier, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
        hier.release();

        MatOfPoint2f biggest = new MatOfPoint2f();
        double max_area = 0;
        for (MatOfPoint i : contours) {
            double area = Imgproc.contourArea(i);
            if (area > 100) {
                MatOfPoint2f m = new MatOfPoint2f(i.toArray());
                double peri = Imgproc.arcLength(m, true);
                MatOfPoint2f approx = new MatOfPoint2f();
                Imgproc.approxPolyDP(m, approx, 0.02 * peri, true);
                if (area > max_area && approx.total() == 4) {
                    biggest = approx;
                    max_area = area;
                }
            }
        }


        // find the outer box
        Mat displayMat = greymat;
        org.opencv.core.Point[] points = biggest.toArray();
        cropped = new Mat();
        int t = 3;
        if (points.length >= 4) {
            // draw the outer box
            Imgproc.line(displayMat, new org.opencv.core.Point(points[0].x, points[0].y), new org.opencv.core.Point(points[1].x, points[1].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new org.opencv.core.Point(points[1].x, points[1].y), new org.opencv.core.Point(points[2].x, points[2].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new org.opencv.core.Point(points[2].x, points[2].y), new org.opencv.core.Point(points[3].x, points[3].y), new Scalar(255, 0, 0), 2);
            Imgproc.line(displayMat, new org.opencv.core.Point(points[3].x, points[3].y), new org.opencv.core.Point(points[0].x, points[0].y), new Scalar(255, 0, 0), 2);
            // crop the image
            Rect R = new Rect(new org.opencv.core.Point(points[0].x - t, points[0].y - t), new org.opencv.core.Point(points[2].x + t, points[2].y + t));
          //  if (displayMat.width() > 1 && displayMat.height() > 1) {
      //          cropped = new Mat(displayMat, R);
        //    }
        }
        Utils.matToBitmap(displayMat, graybitmap);
        viewImage.setImageBitmap(graybitmap);

//--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
//---------------------------#TESSERACT DIGIT--OCR--RECOGNITION---------------

        //        pd.show();
//        if (cropped.width() < 1 || cropped.height() < 1) {
//            finish();
//        }
//
//
//        // ImageView iv = (ImageView) findViewById(R.id.solve_img);
//        viewImage.setVisibility(View.VISIBLE);
//        // initialize the TessBase
//        tessBaseApi = new TessBaseAPI();
//        tessBaseApi.init(DATA_PATH, lang);
//        tessBaseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);
//        tessBaseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "123456789");
//        tessBaseApi.setVariable("classify_bin_numeric_mode", "1");
//
//        Mat output = cropped.clone();
//
//        int SUDOKU_SIZE = 9;
//        int IMAGE_WIDTH = output.width();
//        int IMAGE_HEIGHT = output.height();
//        double PADDING = IMAGE_WIDTH / 25;
//        int HSIZE = IMAGE_HEIGHT / SUDOKU_SIZE;
//        int WSIZE = IMAGE_WIDTH / SUDOKU_SIZE;
//        DigitRecognizer digitRecognizer = new DigitRecognizer();
//        digitRecognizer.ReadMNISTData();
//
//        int[][] sudos = new int[SUDOKU_SIZE][SUDOKU_SIZE];
//
//        // Divide the image to 81 small grid and do the digit recognition
//        for (int y = 0, iy = 0; y < IMAGE_HEIGHT - HSIZE; y += HSIZE, iy++) {
//            for (int x = 0, ix = 0; x < IMAGE_WIDTH - WSIZE; x += WSIZE, ix++) {
//                sudos[iy][ix] = 0;
//                int cx = (x + WSIZE / 2);
//                int cy = (y + HSIZE / 2);
//                org.opencv.core.Point p1 = new org.opencv.core.Point(cx - PADDING, cy - PADDING);
//                org.opencv.core.Point p2 = new org.opencv.core.Point(cx + PADDING, cy + PADDING);
//                Rect R = new Rect(p1, p2);
//                Mat digit_cropped = new Mat(output, R);
//                Imgproc.GaussianBlur(digit_cropped, digit_cropped, new Size(5, 5), 0);
//                Imgproc.rectangle(output, p1, p2, new Scalar(0, 0, 0));
//                Bitmap digit_bitmap = Bitmap.createBitmap(digit_cropped.cols(), digit_cropped.rows(), Bitmap.Config.ARGB_8888);
//                Utils.matToBitmap(digit_cropped, digit_bitmap);
//
//                tessBaseApi.setImage(digit_bitmap);
//                String recognizedText = tessBaseApi.getUTF8Text();
//                if (recognizedText.length() == 1) {
//                    sudos[iy][ix] = Integer.valueOf(recognizedText);
//
//                }
//                Imgproc.putText(output, recognizedText, new org.opencv.core.Point(cx, cy), 1, 3.0f, new Scalar(0));
//                Log.i("testing", "" + Arrays.toString(sudos[iy]));
//                tessBaseApi.clear();
//            }
//            Log.i("testing", "" + Arrays.toString(sudos[iy]));
//        }
//
//        tessBaseApi.end();
//
//
////         Testing data

        test_sudo = new int[][]{{5, 0, 0, 0, 1, 0 ,0 ,0 ,4},
                {2, 7, 4, 0, 0, 0, 6, 0, 0},
                {0, 8, 0, 9, 0, 0, 0, 0, 0},
                {8, 1, 0, 4, 6, 0, 3, 0, 2},
                {0, 0, 2, 0, 3, 0, 1, 0, 0},
                {0, 0, 6, 0, 9, 1, 0,5, 8},
                {0, 0, 0, 0 ,0 ,3, 0, 1, 0},
                {0, 0, 5, 0, 0, 0, 9, 2, 7},
                {1, 0, 0, 0, 2, 0, 0, 0, 3}};

//        // Copy the captured array
//        int[][] test_sudo = Arrays.copyOf(sudos, sudos.length);
//
        // make a copy of the captured array
        int[][] temp = new int[9][9];
        for (int i = 0; i < 9; i++) {
            for (int y = 0; y < 9; y++) {
                temp[i][y] = test_sudo[i][y];

            }
            Log.i("Temp data ", " " + Arrays.toString(temp[i]));
        }
        SudokuGrid.updateSolution(test_sudo);
        // Solve the puzzle
        Solver solver = new Solver(test_sudo, this);
        result = solver.mainSolver();
//        SudokuGrid.updateSolution(result);
//        pd.dismiss();
//
    }

    /**
     * Soduku Solver
     */
    private class Solver {

        int[][] puzzle;
        Context context;

        public Solver(int[][] puzzle, Context context) {
            this.puzzle = puzzle;
            this.context = context;
        }

        public int check(int row, int col, int num) {

            int rowStart = (row / 3) * 3;
            int colStart = (col / 3) * 3;
            int i;
            for (i = 0; i < 9; i++) {
                if (puzzle[row][i] == num) {
                    return 0;
                }
                if (puzzle[i][col] == num) {
                    return 0;
                }
                if (puzzle[rowStart + (i % 3)][colStart + (i / 3)] == num) {
                    return 0;
                }
            }
            return 1;
        }


        public int solve(int row, int col) {
            if (row < 9 && col < 9) {
                if (puzzle[row][col] != 0) {
                    if ((col + 1) < 9)
                        return solve(row, col + 1);
                    else if ((row + 1) < 9)
                        return solve(row + 1, 0);
                    else
                        return 1;
                } else {
                    for (int i = 0; i < 9; i++) {
                        if (check(row, col, i + 1) == 1) {
                            puzzle[row][col] = i + 1;
                            if (solve(row, col) == 1)
                                return 1;
                            else
                                puzzle[row][col] = 0;
                        }
                    }
                }
                return 0;
            } else return 1;
        }

        public int[][] mainSolver() {
            int[][] result = new int[9][9];

            if (solve(0, 0) == 1) {
                for (int i = 0; i < 9; i++) {
                    for (int j = 0; j < 9; j++) {
                        result[i][j] = puzzle[i][j];
                    }
                }
                String s = "";
                for (int i = 0; i < 9; i++) {
                    s = s + Arrays.toString(puzzle[i]) + " \n";
                }
//                Toast toast = Toast.makeText(context, s, Toast.LENGTH_LONG);
//                toast.show();

            } else {
                Toast toast = Toast.makeText(context, "Not Valid!", Toast.LENGTH_SHORT);
                toast.show();
            }
            return puzzle;
        }
    }

    public static class SudokuGrid {

        //contains reference to the sudoku cells
        private static EditText[][] gridCell;
        //contains value of integer in cells, if blank then 0
        public static int[][] cellValues = new int[9][9];
        private static int cellDimensions;

        public static void initGrid(Context context, GridLayout gridLayout, int dimensions) {
            cellDimensions = dimensions;
            gridCell = initCell(context);
            int i = 0;
            int j;
            for (int a = 0; a < 11; a++) {
                j = 0;
                for (int b = 0; b < 11; b++) {
                    GridLayout.Spec rowSpan = GridLayout.spec(GridLayout.UNDEFINED, 1);
                    GridLayout.Spec colSpan = GridLayout.spec(GridLayout.UNDEFINED, 1);

                    GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(rowSpan, colSpan);

                    Space space = new Space(context);

                    if (((a == 3) || (a == 7)) && ((b == 3) || (b == 7))) {
                        space.setMinimumWidth(cellDimensions / 10);
                        space.setMinimumHeight(cellDimensions / 10);
                        gridLayout.addView(space, layoutParams);
                        continue;
                    }

                    if ((a == 3) || (a == 7)) {
                        space.setMinimumWidth(cellDimensions);
                        space.setMinimumHeight(cellDimensions / 10);
                        gridLayout.addView(space, layoutParams);
                        continue;
                    }

                    if ((b == 3) || (b == 7)) {
                        space.setMinimumWidth(cellDimensions / 10);
                        space.setMinimumHeight(cellDimensions);
                        gridLayout.addView(space, layoutParams);
                        continue;
                    }

                    gridLayout.addView(gridCell[i][j], layoutParams);
                    j++;
                }
                if ((a == 3) || (a == 7)) continue;
                i++;
            }
        }

        //initializes each cell with appropriate settings
        private static EditText[][] initCell(Context context) {
            final EditText[][] sudokuCell = new EditText[9][9];

            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    sudokuCell[i][j] = new EditText(context);
                    sudokuCell[i][j].setCursorVisible(false);
                    sudokuCell[i][j].setBackgroundResource(R.drawable.edit_text_style);
                    sudokuCell[i][j].setMinimumHeight(cellDimensions);
                    sudokuCell[i][j].setMinimumWidth(cellDimensions);
                    sudokuCell[i][j].setTextSize(15);
                    sudokuCell[i][j].setPadding(0, 0, 0, 0);
                    sudokuCell[i][j].setGravity(Gravity.CENTER);
                    sudokuCell[i][j].setClickable(true);
                    sudokuCell[i][j].setFocusable(true);
                    sudokuCell[i][j].setFocusableInTouchMode(true);
                    sudokuCell[i][j].setInputType(InputType.TYPE_CLASS_NUMBER);
                    sudokuCell[i][j].addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        //does not allow any integers other than 1-9
                        @Override
                        public void afterTextChanged(Editable s) {
                            if ((s.length() == 1) && (Integer.parseInt(s.toString()) != 0))
                                return;
                            if (s.length() == 0) return;
                            if (Integer.parseInt(s.toString()) == 0) {
                                s.clear();
                                return;
                            }
                            //if two digit integer is entered, take the last entered digit
                            s.replace(0, s.length(), String.valueOf(s.toString().charAt(s.length() - 1)));
                        }
                    });

                    //change cursor position to end of text
                    sudokuCell[i][j].setOnFocusChangeListener(new View.OnFocusChangeListener() {
                        @Override
                        public void onFocusChange(View v, boolean hasFocus) {
                            if (hasFocus) {
                                final EditText et = (EditText) v;
                                et.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        et.setSelection(et.length());
                                    }
                                });
                            }
                        }
                    });

                    sudokuCell[i][j].setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final EditText et = (EditText) v;
                            et.post(new Runnable() {
                                @Override
                                public void run() {
                                    et.setSelection(et.length());
                                }
                            });
                        }
                    });
                }

            }
            return sudokuCell;
        }

        public static void getCellValues() {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    if (!gridCell[i][j].getText().toString().isEmpty()) {
                        cellValues[i][j] = Integer.parseInt(gridCell[i][j].getText().toString());
                    } else cellValues[i][j] = 0;
                }
            }
        }

        public static boolean getSolution() {
            boolean solnExists = SolveSudoku.solvePuzzle(cellValues);
            if (solnExists) cellValues = SolveSudoku.sudokuGrid;
            Log.d(TAG, String.valueOf(solnExists));
            return solnExists;
        }


        public static void updateSolution(int[][] cellValues) {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    gridCell[i][j].setText(String.valueOf(cellValues[i][j]));
                }
            }
        }

        public static void clearGrid() {
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) {
                    cellValues[i][j] = 0;
                    gridCell[i][j].setText("");
                }
            }
        }
    }

    public static class SolveSudoku {

        public static int[][] sudokuGrid;

        public static boolean solvePuzzle(int[][] grid) {
            sudokuGrid = grid;
            boolean solnExists = checkValidity();
            if (!solnExists) return solnExists;
            solnExists = solve(0, 0);
            return solnExists;
        }

        //check whether no two same numbers exist in each row, column or smaller square
        public static boolean checkValidity() {
            boolean[] poss = new boolean[9];
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) poss[j] = true;
                if (!horizontalCheck(poss, i)) return false;
            }
            for (int i = 0; i < 9; i++) {
                for (int j = 0; j < 9; j++) poss[j] = true;
                if (!verticalCheck(poss, i)) ;
            }
            for (int i = 0; i < 9; i = i + 3) {
                for (int j = 0; j < 9; j = j + 3) {
                    for (int k = 0; k < 9; k++) poss[k] = true;
                    if (!squareCheck(poss, i, j)) return false;
                }
            }
            return true;
        }

        private static boolean horizontalCheck(boolean[] poss, int x) {
            boolean output = true;
            for (int i = 0; i < 9; i++) {
                if (sudokuGrid[x][i] != 0) {
                    if (poss[sudokuGrid[x][i] - 1]) {
                        poss[sudokuGrid[x][i] - 1] = false;
                    } else {
                        output = false;
                    }
                }
            }
            return output;
        }

        private static boolean verticalCheck(boolean[] poss, int y) {
            boolean output = true;
            for (int i = 0; i < 9; i++) {
                if (sudokuGrid[i][y] != 0) {
                    if (poss[sudokuGrid[i][y] - 1]) {
                        poss[sudokuGrid[i][y] - 1] = false;
                    } else output = false;
                }
            }
            return output;
        }

        private static boolean squareCheck(boolean[] poss, int x, int y) {
            boolean output = true;
            int startx, starty;
            startx = (x / 3) * 3;
            starty = (y / 3) * 3;
            for (int i = startx; i < startx + 3; i++) {
                for (int j = starty; j < starty + 3; j++) {
                    if (sudokuGrid[i][j] != 0) {
                        if (poss[sudokuGrid[i][j] - 1]) {
                            poss[sudokuGrid[i][j] - 1] = false;
                        } else output = false;
                    }
                }
            }
            return output;
        }

        public static boolean solve(int x, int y) {
            if (x == 9) return true;
            int ny, nx;
            if (y < 8) {
                ny = y + 1;
                nx = x;
            } else {
                ny = 0;
                nx = x + 1;
            }
            if (sudokuGrid[x][y] != 0) return solve(nx, ny);
            //possible entries for current cell
            boolean[] poss = new boolean[9];
            for (int i = 0; i < 9; i++) poss[i] = true;
            horizontalCheck(poss, x);
            verticalCheck(poss, y);
            squareCheck(poss, x, y);
            for (int i = 0; i < 9; i++) {
                if (poss[i]) {
                    sudokuGrid[x][y] = i + 1;
                    if (solve(nx, ny)) return true;
                }
            }
            sudokuGrid[x][y] = 0;
            return false;
        }
    }
}