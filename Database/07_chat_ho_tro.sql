IF OBJECT_ID(N'dbo.chat_phien', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_phien (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_khach_hang BIGINT NOT NULL,
        id_nhan_vien BIGINT NULL,
        trang_thai VARCHAR(30) NOT NULL,

        ngay_tao DATETIME2 NOT NULL
            CONSTRAINT DF_chat_phien_ngay_tao DEFAULT SYSDATETIME(),

        ngay_cap_nhat DATETIME2 NOT NULL
            CONSTRAINT DF_chat_phien_ngay_cap_nhat DEFAULT SYSDATETIME(),

        ngay_yeu_cau_nhan_vien DATETIME2 NULL,
        ngay_dong DATETIME2 NULL,

        CONSTRAINT FK_chat_phien_khach_hang
            FOREIGN KEY (id_khach_hang)
            REFERENCES dbo.khach_hang(id),

        CONSTRAINT FK_chat_phien_nhan_vien
            FOREIGN KEY (id_nhan_vien)
            REFERENCES dbo.nhan_vien(id),

        CONSTRAINT CK_chat_phien_trang_thai
            CHECK (
                trang_thai IN (
                    'AI_ACTIVE',
                    'WAITING_STAFF',
                    'STAFF_ACTIVE',
                    'CLOSED'
                )
            )
    );

    CREATE INDEX IX_chat_phien_khach_hang
        ON dbo.chat_phien(id_khach_hang, ngay_cap_nhat DESC);

    CREATE INDEX IX_chat_phien_trang_thai
        ON dbo.chat_phien(trang_thai, ngay_cap_nhat DESC);
END;
GO

IF OBJECT_ID(N'dbo.chat_tin_nhan', N'U') IS NULL
BEGIN
    CREATE TABLE dbo.chat_tin_nhan (
        id BIGINT IDENTITY(1,1) PRIMARY KEY,
        id_phien BIGINT NOT NULL,
        loai_nguoi_gui VARCHAR(20) NOT NULL,
        id_nguoi_gui BIGINT NULL,
        ten_nguoi_gui NVARCHAR(150) NULL,
        noi_dung NVARCHAR(MAX) NOT NULL,

        ngay_gui DATETIME2 NOT NULL
            CONSTRAINT DF_chat_tin_nhan_ngay_gui DEFAULT SYSDATETIME(),

        CONSTRAINT FK_chat_tin_nhan_phien
            FOREIGN KEY (id_phien)
            REFERENCES dbo.chat_phien(id),

        CONSTRAINT CK_chat_tin_nhan_loai
            CHECK (
                loai_nguoi_gui IN (
                    'CUSTOMER',
                    'AI',
                    'STAFF',
                    'SYSTEM'
                )
            )
    );

    CREATE INDEX IX_chat_tin_nhan_phien
        ON dbo.chat_tin_nhan(id_phien, ngay_gui ASC);
END;
GO
