package com.smashstep.datn.auth.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FixCaLamViec {

    public static void main(String[] args) throws Exception {
        String url = "jdbc:sqlserver://localhost:1433;databaseName=SmashStep;"
                   + "encrypt=true;trustServerCertificate=true";

        try (Connection conn = DriverManager.getConnection(url, "sa", "123456")) {
            String caSang  = "Ca s\u00e1ng";
            String caChieu = "Ca chi\u1ec1u";
            String caToi   = "Ca t\u1ed1i";

            PreparedStatement ps = conn.prepareStatement(
                "UPDATE ca_lam_viec SET ten_ca = ? WHERE ma_ca = ?");

            ps.setNString(1, caSang);  ps.setString(2, "CA_SANG");  ps.executeUpdate();
            ps.setNString(1, caChieu); ps.setString(2, "CA_CHIEU"); ps.executeUpdate();
            ps.setNString(1, caToi);   ps.setString(2, "CA_TOI");   ps.executeUpdate();

            // In hex để xác nhận không phụ thuộc console encoding
            ResultSet rs = conn.createStatement()
                .executeQuery("SELECT ma_ca, ten_ca FROM ca_lam_viec ORDER BY id");
            while (rs.next()) {
                String val = rs.getNString("ten_ca");
                StringBuilder hex = new StringBuilder();
                for (char c : val.toCharArray()) hex.append(String.format("U+%04X ", (int) c));
                System.out.println(rs.getString("ma_ca") + " hex: " + hex.toString().trim());
            }
        }
    }
}
