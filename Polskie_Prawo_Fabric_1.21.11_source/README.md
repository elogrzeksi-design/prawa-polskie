# Polskie Prawo — Fabric 1.21.11

Humorystyczny mod survivalowy z pozwoleniami, mandatami i podatkami.

## Wymagania
- Minecraft Java Edition 1.21.11
- Fabric Loader 0.18.1 lub nowszy zgodny z 1.21.11
- Fabric API 0.141.1+1.21.11
- Java JDK 21
- Gradle (albo otwarcie folderu w IntelliJ IDEA, które pobierze Gradle)

## Budowanie
W folderze projektu uruchom:
```bat
gradle build
```
Gotowy plik JAR powinien znaleźć się w `build\libs\polskie-prawo-1.0.0.jar`.
Skopiuj go do folderu `.minecraft\mods`.

## Ważne
To jest wersja prototypowa kodu źródłowego, niezweryfikowana kompilacją w tym środowisku. W tej wersji część systemów jest uproszczona:
- pozwolenia są przygotowane jako mechanizm wewnętrzny, ale interfejs urzędnika i wydawanie pozwoleń wymagają dalszego wdrożenia;
- wykrywanie martwych drzew i zakaz wyrzucania przedmiotów na ziemię wymagają dodatkowych zdarzeń;
- zasady rybołówstwa ostrzegają, ale nie cofają śmierci ryby;
- opłata 12% i podatek są prototypowe i powinny być przetestowane na kopii świata.

Projekt korzysta z oficjalnych mapowań Mojang dla 1.21.11. Zależności i wersje bazują na dokumentacji Fabric dla tej wersji.
