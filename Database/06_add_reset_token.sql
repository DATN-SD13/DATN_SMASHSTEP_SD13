-- Source: origin/hung 0eec8c2. Additive only; guard each column independently.
USE [SmashStep];
GO
SET QUOTED_IDENTIFIER ON;
SET ANSI_NULLS ON;
GO
IF COL_LENGTH('dbo.khach_hang', 'reset_token') IS NULL
BEGIN
    ALTER TABLE dbo.khach_hang ADD reset_token VARCHAR(255) NULL;
    PRINT N'Added khach_hang.reset_token';
END;
IF COL_LENGTH('dbo.khach_hang', 'reset_token_expiry') IS NULL
BEGIN
    ALTER TABLE dbo.khach_hang ADD reset_token_expiry DATETIME2 NULL;
    PRINT N'Added khach_hang.reset_token_expiry';
END;
GO
