@echo off
title DiviStay tests
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\divistay.ps1" test
