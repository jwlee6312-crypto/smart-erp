# 🛠️ ASRock H61M/U3S3 P2.40 전용 정밀 재설치 프로토콜

이 가이드는 사장님의 메인보드 **ASRock H61M/U3S3 (BIOS Version P2.40)**의 실제 메뉴 명칭을 100% 동일하게 사용하여 작성되었습니다. 글자 하나라도 틀리지 않게 설계되었습니다.

## Phase 1: BIOS 설정 (UEFI Setup Utility)

서버를 켜고 **F2**를 눌러 진입한 후, 아래 순서대로 설정을 확인/변경합니다.

### 1. Advanced 탭 -> Storage Configuration
*   **SATA Mode Selection**: `[AHCI]` (반드시 확인)

### 2. Advanced 탭 -> USB Configuration
*   **Legacy USB Support**: `[Enabled]`

### 3. Boot 탭
*   **Fast Boot**: `[Disabled]`
*   **PCI ROM Priority**: `[Legacy ROM]` (가장 중요: `EFI Compatible ROM`이 아니어야 합니다.)

### 4. Exit 탭
*   **Save Changes and Exit**: 선택 후 `Yes`를 눌러 저장하고 나옵니다.

---

## Phase 2: 부팅 장치 선택 (F11)

서버가 재부팅될 때 **F11**을 연타하여 **[Select Boot Device]** 창을 띄웁니다.

*   목록에서 사장님의 USB 장치명을 선택하되, **앞에 `UEFI:` 가 붙지 않은 항목**을 선택합니다.
*   예: `USB: VendorCoProductCode` (O) / `UEFI: VendorCoProductCode` (X)

---

## Phase 3: 정밀 설치 (Ubuntu Server 24.04)

설치 화면이 나타나면 아래 순서대로 진행합니다.

### 1. 초기 설정
*   **Language**: `English`
*   **Keyboard**: `English (US)`
*   **Network**: 유선 연결 확인 후 `Done`

### 2. Storage Configuration (가장 정밀한 단계)
1.  **`Custom storage layout`** 을 선택하고 엔터를 누릅니다.
2.  **Samsung SSD 860 EVO (sdb)** 를 선택하고 엔터를 눌러 **`Wipe`** 를 수행합니다.
3.  SSD 아래에 생긴 **`FREE SPACE`** 를 선택하고 엔터를 눌러 **`Add Partition`** 을 선택합니다.
    *   **Size**: (비워둠 - 전체 용량 사용)
    *   **Format**: `ext4`
    *   **Mount**: `/`
4.  하단의 **`Done`** 을 누르고 빨간 경고창에서 **`Continue`** 를 선택합니다.

---

## Phase 4: 최종 부팅 및 네트워크 가동

1.  설치 완료 후 **`Reboot Now`** 선택.
2.  "Please remove the installation medium" 메시지 확인 후 **USB 제거 및 Enter**.
3.  부팅 후 로그인 화면이 나오면 성공입니다.
