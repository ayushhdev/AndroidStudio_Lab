# ImageSlice: Image Grid and Cropping App

| | |
|---|---|
| **Name** | Ayush Kumar |
| **USN** | 25MCAR0224 |
| **Concepts** | Photo Picker · custom `View` drawing · `SeekBar` · `Bitmap` cropping · `GridView` · explicit Intents |

ImageSlice loads a photo from the gallery, divides it into a grid you can adjust, lets you tap a
box to select it, and opens that box as a cropped image on a second screen.

## Features

1. **Image selection:** the system Photo Picker (`ActivityResultContracts.PickVisualMedia`) picks
   an image without asking for a storage permission.
2. **Grid display:** two `SeekBar`s set the number of rows and columns from 1 to 10. A custom
   `GridImageView` draws the grid lines over the image.
3. **Box selection:** tapping the image highlights the box under your finger and shows its row and
   column.
4. **Cropped view:** **Open Box** starts `SliceActivity` with an explicit Intent. It crops the
   selected box with `Bitmap.createBitmap` (`ImageSlicer`) and shows it large.
5. **All boxes:** a `GridView` below the large image shows every piece. Tap one to switch to it.

## Project structure

```
app/src/main/java/com/ayush/imageslice/
├── MainActivity.kt    Picker, grid SeekBars, selection
├── GridImageView.kt   ImageView that draws the grid and handles taps
├── ImageSlicer.kt     Bitmap cropping helpers
└── SliceActivity.kt   Cropped box and GridView of all boxes
```

## Running

Open the `ImageSlice` folder in Android Studio and run it on a device or
emulator with API 29 (Android 10) or later.

```bash
./gradlew assembleDebug
```
