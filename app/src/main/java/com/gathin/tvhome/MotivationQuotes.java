package com.gathin.tvhome;

/** Short family-friendly motivational lines for the home hero. Offline, no network, no tracking. */
final class MotivationQuotes {
    static final long ROTATE_MS = 30_000L;
    static final String[] LINES = {
            "Pengembaraan bermula di sini.",
            "Wira sebenar tak pernah berhenti mencuba.",
            "Langkah kecil hari ini, kejayaan besar esok.",
            "Jangan takut salah — itu tandanya kita sedang belajar.",
            "Berani bermimpi, rajin berusaha.",
            "Senyum dulu, baru semua jadi lebih mudah.",
            "Kuasa paling hebat ialah kebaikan.",
            "Setiap hari ialah peluang untuk jadi lebih baik.",
            "Usaha tangga kejayaan.",
            "Bersatu kita teguh, bercerai kita roboh.",
            "Hormati ibu bapa, berkat hidup kita.",
            "Solat dulu, main kemudian.",
            "Baca, belajar, dan terus bertanya.",
            "Kalau jatuh, bangun semula. Itu baru wira!",
            "Tolong kawan, hati pun gembira.",
            "Jujur itu hebat, lebih hebat dari kuasa super.",
            "Sabar sikit, kejayaan sedang dalam perjalanan.",
            "Bersyukur dengan apa yang ada.",
            "Hari ini, jadi wira untuk keluarga.",
            "Mula dengan Bismillah, akhiri dengan Alhamdulillah.",
            "Rehat cukup, otak pun cergas.",
            "Kecil-kecil cili padi — kecil tapi berani!",
            "Satu kebaikan kecil boleh ubah hari seseorang.",
            "Terus cuba sampai berjaya.",
            "Ilmu itu pelita hidup.",
    };
    private MotivationQuotes() {}
    static int index(long epochMillis) {
        return (int) Math.floorMod(Math.floorDiv(epochMillis, ROTATE_MS), (long) LINES.length);
    }
    static String at(long epochMillis) { return LINES[index(epochMillis)]; }
}
