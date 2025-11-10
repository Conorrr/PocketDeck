package io.restall.sharedex.classifier;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

public record AppConfig(
        Path cardImageDir,
        Path pHashBinary,
        Path orbDatabaseBin,
        Path uploadDir,
        Path rarityMapPath,
        Path cardListPath,
        Path rawCardDetailsPath,
        Path allCardsPath
) {

    private static String CARD_IMAGE_DIR = "CARD_IMAGE_DIR";
    private static String PHASH_BINARY_PATH = "PHASH_BINARY_PATH";
    private static String ORB_BIN_PATH = "ORB_BIN_PATH";
    private static String UPLOAD_DIR = "UPLOAD_DIR";
    private static String RARITY_MAP_PATH = "RARITY_MAP_PATH";
    private static String CARD_LIST_PATH = "CARD_LIST_PATH";
    private static String RAW_CARD_DETAILS_PATH = "RAW_CARD_DETAILS_PATH";
    private static String ALL_CARDS_PATH = "ALL_CARDS_PATH";


    public static AppConfig fromEnv() {
        return new AppConfig(
                pathFromEnv(CARD_IMAGE_DIR),
                pathFromEnv(PHASH_BINARY_PATH),
                pathFromEnv(ORB_BIN_PATH),
                pathFromEnv(UPLOAD_DIR),
                pathFromEnv(RARITY_MAP_PATH),
                pathFromEnv(CARD_LIST_PATH),
                pathFromEnv(RAW_CARD_DETAILS_PATH),
                pathFromEnv(ALL_CARDS_PATH)
        );
    }

    private static Path pathFromEnv(String envVar) {
        return Optional.ofNullable(System.getenv(envVar)).map(Paths::get).orElse(null);
    }

}
