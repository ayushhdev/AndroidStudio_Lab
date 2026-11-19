# QRVault: Contact Sharing with a QR Code Generator

| | |
|---|---|
| **Name** | Ayush Kumar |
| **USN** | 25MCAR0224 |
| **Concepts** | Contacts Content Provider · ZXing QR codes · Camera · SQLite · Intents |

QRVault reads contacts from the device, turns the selected contact into a QR code, lets you save
that code to the gallery or share it, and keeps every generated code in an SQLite history.

## Features

| # | Requirement | How it is implemented |
|---|---|---|
| 1 | **Fetch contacts** | `ContactsRepository` queries `ContactsContract.Contacts` for names, then queries `ContactsContract.Data` once for every phone number and email. It uses `ContentResolver` on a background thread and asks for the `READ_CONTACTS` runtime permission. |
| 2 | **Contact selection** | `MainActivity` lists the contacts in a `RecyclerView` (`ContactAdapter`) with initials avatars and a search box that filters by name, phone or email. Tapping a contact opens `QrActivity` through an explicit Intent. |
| 3 | **QR code generation** | `VCard` builds a vCard 3.0 payload with the name, phones and emails. `QrCodeGenerator` encodes it with ZXing's `QRCodeWriter` (UTF-8, error correction level M) into a `Bitmap`. This runs automatically when the screen opens. |
| 4 | **Display and sharing** | `QrActivity` shows the code and the contact's details. **Save to Gallery** writes a PNG card to `Pictures/QRVault` through `MediaStore`, so no storage permission is needed. **Share** sends the PNG with an `ACTION_SEND` Intent, using a `FileProvider` `content://` URI. |
| 5 | **QR code history** | `QrHistoryDbHelper` (`SQLiteOpenHelper`) stores the contact details, the vCard payload and the QR image as a PNG BLOB. `HistoryActivity` lists past codes with thumbnails. You can reopen an entry, delete it (with Undo) or clear the whole history. |
| + | **Camera scan** | **Scan QR** in the toolbar opens the ZXing camera scanner. If it reads a vCard, QRVault shows the contact and offers **Add to Contacts**, which uses a `ContactsContract.Intents.Insert` Intent. |

## Project structure

```
app/src/main/java/com/example/qrvault/
├── MainActivity.kt        Contact list, search, permissions, scanner
├── QrActivity.kt          QR display, save to gallery, share
├── HistoryActivity.kt     SQLite history list
├── ContactsRepository.kt  Contacts Content Provider queries
├── VCard.kt               vCard build and parse
├── QrCodeGenerator.kt     ZXing encoding and the shareable card image
├── QrHistoryDbHelper.kt   SQLiteOpenHelper for the history table
├── QrImageExport.kt       MediaStore save and FileProvider share
├── ContactAdapter.kt / HistoryAdapter.kt
└── Ui.kt                  Insets, avatar and date helpers
```

### SQLite schema

```sql
CREATE TABLE qr_history (
    _id          INTEGER PRIMARY KEY AUTOINCREMENT,
    contact_name TEXT NOT NULL,
    phones       TEXT NOT NULL,      -- newline-separated
    emails       TEXT NOT NULL,      -- newline-separated
    payload      TEXT NOT NULL UNIQUE,
    qr_image     BLOB NOT NULL,      -- PNG bytes
    created_at   INTEGER NOT NULL
);
```

If you generate a code for the same contact again, the app does not add a duplicate row. It moves
the existing entry to the top of the history.

## Libraries

- `com.google.zxing:core:3.5.3` encodes the QR codes
- `com.journeyapps:zxing-android-embedded:4.3.0` provides the camera scanner
- AndroidX RecyclerView, Material Components

## Running

Open the `QRVault` folder in Android Studio and run it on a device or emulator with
API 29 or later. To try it on an emulator, first add a couple of contacts in the Contacts app.

```bash
./gradlew assembleDebug        # build the APK
./gradlew testDebugUnitTest    # vCard unit tests
```
