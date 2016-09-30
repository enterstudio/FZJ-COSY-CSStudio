package com.cosylab.fzj.cosy.oc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DataLoader {

    private static final int NAME_INDEX = 0;
    private static final int POSITION_INDEX = 1;
    private static final int CORRECTOR_ORIENTATION_INDEX = 3;
    private static final String DATA_DELIMITER = " ";

    public static void main(String[] args) {
        DataLoader dl = new DataLoader();
        dl.loadLatticeElements().forEach(e -> System.out.println(e.getName() + " " + e.getType() + " " + e.getPosition()));
    }

    public List<LatticeElement> loadLatticeElements() {
        List<LatticeElement> elements = new ArrayList<>();
        try {
            //TODO file loading
            String bpmsFilePath = "C:\\Cosylab\\Projects\\FZJ-COSY\\env-setup\\CS-Studio\\plugins\\com.cosylab.fzj.cosy.oc\\bpms.twiss";
            String correctorsFilePath = "C:\\Cosylab\\Projects\\FZJ-COSY\\env-setup\\CS-Studio\\plugins\\com.cosylab.fzj.cosy.oc\\correctors.twiss";
            elements.addAll(getBpms(getFileContent(bpmsFilePath)));
            elements.addAll(getCorrectors(getFileContent(correctorsFilePath)));

            elements.get(0).addPV(LatticeElementPVType.VERTICAL_CORRECTOR_CORRECTION, "SPTC:PPS:Heartbeat");
        } catch (IOException ioe) {
            //TODO
            ioe.printStackTrace();
        }
        return elements;
    }

    private List<LatticeElement> getBpms(List<String> fileContent) {
        List<LatticeElement> bpms = new ArrayList<>();
        fileContent.forEach(line -> {
            String[] splittedLine = line.split(DATA_DELIMITER);
            if (splittedLine.length >= 2) {
                String name = splittedLine[NAME_INDEX];
                double position = Double.parseDouble(splittedLine[POSITION_INDEX]); //TODO NumberFormatExcpetion

                bpms.add(new LatticeElement(name, position, LatticeElementType.BPM));
            }
        });
        return bpms;
    }

    private List<LatticeElement> getCorrectors(List<String> fileContent) {
        List<LatticeElement> correctors = new ArrayList<>();
        fileContent.forEach(line -> {
            String[] splittedLine = line.split(DATA_DELIMITER);
            if (splittedLine.length >= 4) {
                String name = splittedLine[NAME_INDEX];
                double position = Double.parseDouble(splittedLine[POSITION_INDEX]); //TODO NumberFormatExcpetion
                LatticeElementType type = null;
                String orientation = splittedLine[CORRECTOR_ORIENTATION_INDEX];
                if (orientation.equals("horizontal")) {
                    type = LatticeElementType.HORIZONTAL_CORRECTOR;
                } else if (orientation.equals("vertical")) {
                    type = LatticeElementType.VERTICAL_CORRECTOR;
                } else if (orientation.equals("horizontal/vertical")) {
                    type = LatticeElementType.HORIZONTAL_VERTICAL_CORRECTOR;
                }
                // TODO if type == null -> exception
                correctors.add(new LatticeElement(name, position, type));
            }
        });
        return correctors;
    }

    private List<String> getFileContent(String filePath) throws IOException {
        List<String> fileContent = new ArrayList<>();
        try (Stream<String> fileStream = Files.lines(Paths.get(filePath))) {
            fileStream.forEach(line -> {
                if (isElementLine(line)) {
                    String parsedLine = line.replaceAll("\\s+", DATA_DELIMITER).replaceAll("\"", "").trim();
                    fileContent.add(parsedLine);
                }
            });
        }
        return fileContent;
    }

    private boolean isElementLine(String line) {
        return line != null && !line.isEmpty() && !line.startsWith("@") && !line.startsWith("*") && !line.startsWith("$");
    }
}