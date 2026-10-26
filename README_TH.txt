ASCENT FITNESS RPG v3.4 — ชุดสร้าง Android APK (จากซอร์สจริง)
============================================================

สถานะ: ยังไม่ใช่ APK สำเร็จรูป / ยังไม่ได้คอมไพล์ APK ใน ChatGPT
แพ็กนี้เป็นโปรเจ็กต์ที่เตรียมให้บิลด์ APK แบบ Debug ได้
เพราะเครื่องมือ Android SDK และ Gradle ไม่พร้อมในระบบที่สร้างไฟล์นี้

ทางเลือก A: สร้าง APK บน GitHub โดยไม่ต้องติดตั้ง Android Studio
------------------------------------------------------------
1. แตกไฟล์ ZIP นี้บนคอม
2. เข้า github.com สร้าง Repository ใหม่ (เลือก Private ได้)
3. อัปโหลดเนื้อหาทั้งโฟลเดอร์นี้ไปที่ราก Repository:
   android/, web/, .github/ และไฟล์อื่น ๆ
   ต้องให้ไฟล์ .github/workflows/build-android-apk.yml อยู่ในเส้นทางนี้จริง
   สำหรับบางหน้าจอ Upload Files อาจไม่รองรับโฟลเดอร์ที่ขึ้นต้นด้วยจุด
   ถ้าจำเป็นให้สร้าง workflow ด้วย GitHub Editor ภายในหน้า Actions
4. เปิดแท็บ Actions เลือก "Build ASCENT Android APK"
5. คลิก Run workflow ถ้ามีปุ่ม; การ Push สู่สาขา main ก็ทำงานได้อัตโนมัติ
6. เมื่อสถานะ Build สีเขียว เปิดรายการ Run และดาวน์โหลด Artifacts
   ชื่อ ASCENT-Fitness-v3.4-Android-debug-APK
7. แตก ZIP ที่ GitHub ดาวน์โหลดมา จะพบ app-debug.apk
8. ส่ง APK ไปที่ Android เปิดไฟล์และอนุญาตติดตั้งจากแหล่งนี้

ทางเลือก B: ใช้ Android Studio ใน Windows
------------------------------------------
1. ติดตั้ง Android Studio และ Android SDK Platform 35 / Build Tools 35
2. File > Open > เลือกโฟลเดอร์ android ภายในแพ็กนี้
3. Sync Gradle: ใช้ JDK 17 และ Gradle 8.9
   (ถ้ามีปัญหาโฟลเดอร์นี้ไม่มี Gradle wrapper ให้ตั้ง Gradle Distribution
    จาก Android Studio หรือใช้วิธี GitHub Actions ที่จัดไว้ให้)
4. เลือกเมนู Build > Build Bundle(s) / APK(s) > Build APK(s)
5. เมื่อผ่าน จะได้ android/app/build/outputs/apk/debug/app-debug.apk

ข้อมูลสำคัญ
-----------
- APK เป็น debug-signed เหมาะสำหรับใช้ส่วนตัว ไม่ใช่รุ่นลง Play Store
- ถ้าบิลด์ debug จากคนละคอม / runner หลายครั้ง ลายเซ็นอาจต่างกัน
  อาจต้องถอนแอปเดิมก่อนติดตั้งใหม่ (สำรองข้อมูลก่อน)
- ชุด Android บรรจุหน้าจอเว็บ v3.4 ใน assets/index.html ติดตั้งใช้งาน
  บน Android ได้โดยไม่ต้องเปิด Firebase URL เป็นหน้าแรก
- OCR ใบเสร็จอาจต้องใช้อินเทอร์เน็ตเพื่อโหลด Tesseract.js ครั้งแรก
- GPS เบื้องหลัง เสียงแจ้งเตือน และวิดเจ็ต ยังไม่ผ่านการทดสอบบนมือถือจริง
  Android อาจจำกัดการทำงาน/แจ้งเตือนตามการตั้งค่าระบบ
- ข้อมูลใน PWA กับ APK แยกกัน ให้ Export JSON จากเว็บ และ Import
  ภายใน APK ถ้าต้องการย้ายประวัติ
- แนะนำให้สำรองข้อมูลก่อนอัปเดตแอปทุกครั้ง
- เมื่อใช้การวิ่ง GPS ต้องอนุญาตตำแหน่งและ Notifications ใน Android

ไฟล์สำคัญ
android/       โค้ด Android ที่ใช้สร้าง APK
web/           เว็บแอปเดิมสำหรับ Firebase
.github/workflows/build-android-apk.yml  เวิร์กโฟลว์คอมไพล์บน GitHub
BUILD_APK_WINDOWS.bat   ใช้กรณีเครื่องมี Gradle/Android SDK แล้ว
