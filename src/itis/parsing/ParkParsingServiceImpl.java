package itis.parsing;

import itis.parsing.annotations.FieldName;
import itis.parsing.annotations.MaxLength;
import itis.parsing.annotations.NotBlank;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.util.*;

public class ParkParsingServiceImpl implements ParkParsingService {
    HashMap<String, String> parsedData;

    //Парсит файл в обьект класса "Park", либо бросает исключение с информацией об ошибках обработки
    @Override
    public Park parseParkData(String parkDatafilePath) throws ParkParsingException{
        try{
           parsedData = parseFile(parkDatafilePath);

            Class<Park> parkClass = Park.class;
            Constructor<? extends Park> constructor = parkClass.getDeclaredConstructor();
            constructor.setAccessible(true);
            Park park = constructor.newInstance();

            Field[] fields = parkClass.getDeclaredFields();

            try{
                return processAnnotations(fields, park);

            } catch (ParkParsingException q){
                q.printStackTrace();
                q.getValidationErrors().stream().
                        map(s -> s.getFieldName() + " " + s.getValidationError()).
                        forEach(System.out::println);
            }

        } catch (NoSuchMethodException e){
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }


        return null;
    }

    private HashMap<String, String> parseFile(String path){
        try{
            HashMap<String, String> parsedData = new HashMap<>();

            File file = new File(path);
            FileReader reader = new FileReader(file);
            BufferedReader bf = new BufferedReader(reader);
            String result;

            while((result = bf.readLine()) != null){
                if(!result.equals("***")){
                    String[] line = result.split(": ");
                    String key = line[0].substring(1, line[0].length() - 1);
                    String value = line[1].substring(1, line[1].length() - 1);

                    parsedData.put(key, value);
                }
            }

            return parsedData;

        } catch (FileNotFoundException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private Park processAnnotations(Field[] fields, Park park) throws IllegalAccessException, ParkParsingException {
        for(Field field : fields){
            ArrayList<ParkParsingException.ParkValidationError> errors = new ArrayList<>();

            for(Annotation annotation : field.getAnnotations()){
                if(annotation instanceof NotBlank){
                    if(parsedData.get(field.getName()).equals("") || parsedData.get(field.getName()) == null){
                        errors.add(new ParkParsingException.ParkValidationError(field.getName(),
                                "The value is null or empty string"));
                    } else{
                        field.setAccessible(true);

                        if(field.getName().equals("foundationYear")){
                            LocalDate dt = LocalDate.parse(parsedData.get("foundationYear"));

                            field.set(park, dt);
                        } else{
                            field.set(park, parsedData.get(field.getName()));
                        }
                    }
                }

                if(annotation instanceof MaxLength){
                    if(parsedData.get(field.getName()).length() > ((MaxLength) annotation).value()){
                        errors.add(new ParkParsingException.ParkValidationError(field.getName(),
                                "The length of the value is too long"));
                    } else{
                        field.setAccessible(true);
                        field.set(park, parsedData.get(field.getName()));
                    }
                }

                if(annotation instanceof FieldName){
                    field.setAccessible(true);
                    field.set(park, parsedData.get(((FieldName) annotation).value()));
                }
            }

            if(errors.size() > 0){
                throw new ParkParsingException("Can't parse this file", errors);
            }
        }

        return park;
    }

}
