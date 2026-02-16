const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

// 🎭 MASTER KEY AZURA (Sesuai dengan hasil XOR di C++)
// Kunci ini hanya ada di server Google, mustahil diintip dari luar.
const AZURA_MASTER_KEY = "AZURA_SECURE"; 

/**
 * 🛡️ Fungsi secureCheckIn
 * Inilah "Syariat Cloud" yang memvalidasi absensi sebelum masuk ke Firestore.
 */
exports.secureCheckIn = functions.https.onCall(async (data, context) => {
    // 1. Ambil payload yang dikirim dari Kotlin (ViewModel)
    const { studentId, name, distance, isoKey, className, grade } = data;

    // ==========================================
    // 🛑 GERBANG 1: Validasi Jati Diri (ISO Key)
    // ==========================================
    // Memastikan request ini datang dari kodingan C++ asli Azura Tech.
    if (isoKey !== AZURA_MASTER_KEY) {
        console.error(`[SECURITY BREACH] Kunci Palsu Terdeteksi! ID: ${studentId}`);
        throw new functions.https.HttpsError(
            'permission-denied', 
            'Akses Ditolak: Aplikasi Tidak Dikenal atau Modifikasi!'
        );
    }

    // ==========================================
    // 🛑 GERBANG 2: Validasi Clean Salt (Bumbu 0.00001)
    // ==========================================
    // C++ kita sudah diprogram untuk memaksa angka terakhir jadi '1'.
    const distString = parseFloat(distance).toFixed(5);
    if (!distString.endsWith('1')) {
        console.error(`[SECURITY BREACH] Manipulasi Jarak AI Terdeteksi: ${distString}`);
        throw new functions.https.HttpsError(
            'invalid-argument', 
            'Akses Ditolak: Integritas Algoritma Biometrik Dirusak!'
        );
    }

    // ==========================================
    // ✅ LULUS UJIAN: Catat ke Database Firestore
    // ==========================================
    const record = {
        studentId: studentId,
        name: name,
        className: className || "General",
        gradeName: grade || "-",
        timestamp: admin.firestore.FieldValue.serverTimestamp(), // Waktu murni dari server Google
        status: 'PRESENT',
        distanceScore: distance,
        verifiedBy: 'AzuraCloudShield' // Cap pengesahan dari Cloud
    };

    try {
        // Data disimpan ke collection 'attendance_logs'
        await admin.firestore().collection('attendance_logs').add(record);
        return { 
            status: "SUCCESS", 
            message: `Absen ${name} Sah dan Terverifikasi Server!` 
        };
    } catch (error) {
        console.error("Gagal tulis Firestore:", error);
        throw new functions.https.HttpsError('internal', 'Server gagal mencatat absensi.');
    }
});