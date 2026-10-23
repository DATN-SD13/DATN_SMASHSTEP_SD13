IF OBJECT_ID(N'dbo.chat_tin_nhan', N'U') IS NOT NULL
   AND COL_LENGTH('dbo.chat_tin_nhan', 'metadata_json') IS NULL
BEGIN
    ALTER TABLE dbo.chat_tin_nhan
    ADD metadata_json NVARCHAR(MAX) NULL;
END;
GO
