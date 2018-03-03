/*
 *     This file is part of ToroDB.
 *
 *     ToroDB is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     ToroDB is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with ToroDB. If not, see <http://www.gnu.org/licenses/>.
 *
 *     Copyright (c) 2014, 8Kdata Technology
 *     
 */

package com.torodb.config.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Path;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.slf4j.LoggerFactory;

import com.beust.jcommander.internal.Console;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonMappingException.Reference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.google.common.base.Charsets;
import com.torodb.CliConfig;
import com.torodb.config.model.Config;
import com.torodb.config.model.backend.mysql.MySQL;
import com.torodb.config.model.backend.postgres.Postgres;

import ch.qos.logback.classic.Logger;

public class ConfigUtils {

    private static final Logger LOGGER = (Logger) LoggerFactory.getLogger(ConfigUtils.class);
    
    public static Config readConfig(CliConfig cliConfig) throws FileNotFoundException, JsonProcessingException,
            IOException, JsonParseException, IllegalArgumentException, Exception {
        try {
            return ConfigUtils.uncatchedReadConfig(cliConfig);
        } catch(JsonMappingException jsonMappingException) {
            throw ConfigUtils.transformJsonMappingException(jsonMappingException);
        }
    }
    
    private static ObjectMapper mapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        
        configMapper(objectMapper);
        
        return objectMapper;
    }
    
    private static YAMLMapper yamlMapper() {
        YAMLMapper yamlMapper = new YAMLMapper();
        
        configMapper(yamlMapper);
        
        return yamlMapper;
    }
    
    private static XmlMapper xmlMapper() {
        XmlMapper xmlMapper = new XmlMapper();
        
        configMapper(xmlMapper);
        
        return xmlMapper;
    }
    
    private static void configMapper(ObjectMapper objectMapper) {
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, true);
        objectMapper.configure(Feature.ALLOW_COMMENTS, true);
        objectMapper.configure(Feature.ALLOW_YAML_COMMENTS, true);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.setSerializationInclusion(Include.NON_NULL);
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
	private static Config uncatchedReadConfig(final CliConfig cliConfig) throws Exception {
		ObjectMapper objectMapper = mapper();
		
		Config defaultConfig = new Config();
		if (cliConfig.getBackend() != null) {
		    defaultConfig.getBackend().setBackendImplementation(
		            CliConfig.getBackendClass(cliConfig.getBackend()).newInstance());
		}
		JsonNode configNode = objectMapper.valueToTree(defaultConfig);
		
		if (cliConfig.hasConfFile() || cliConfig.hasXmlConfFile()) {
			ObjectMapper mapper = null;
			InputStream inputStream = null;
			if (cliConfig.hasConfFile()) {
				mapper = yamlMapper();
				inputStream = cliConfig.getConfInputStream();
			} else if (cliConfig.hasXmlConfFile()) {
				mapper = xmlMapper();
				inputStream = cliConfig.getXmlConfInputStream();
			}

			if (inputStream != null) {
		        Config config = mapper.readValue(inputStream, Config.class);
		        configNode = mapper.valueToTree(config);
			}
		}

		if (cliConfig.getParams() != null) {
			YAMLMapper yamlMapper = yamlMapper();
			for (String paramPathValue : cliConfig.getParams()) {
				int paramPathValueSeparatorIndex = paramPathValue.indexOf('=');
				String pathAndProp = paramPathValue.substring(0, paramPathValueSeparatorIndex);

				if (pathAndProp.startsWith("/")) {
					pathAndProp = pathAndProp.substring(1);
				}

				pathAndProp = "/" + pathAndProp;

				String value = paramPathValue.substring(paramPathValueSeparatorIndex + 1);

				mergeParam(yamlMapper, configNode, pathAndProp, value);
			}
		}

		Config config = objectMapper.treeToValue(configNode, Config.class);

		validateBean(config);

		return config;
	}

    public static IllegalArgumentException transformJsonMappingException(JsonMappingException jsonMappingException) {
        JsonPointer jsonPointer = JsonPointer.compile("/config");
        for (Reference reference : jsonMappingException.getPath()) {
            jsonPointer = jsonPointer.append(JsonPointer.compile("/" + reference.getFieldName()));
        }
        return new IllegalArgumentException("Validation error at " + jsonPointer + ": " + jsonMappingException.getMessage());
    }

    public static Config readConfigFromYaml(String yamlString) throws JsonProcessingException, IOException {
        ObjectMapper objectMapper = mapper();
        YAMLMapper yamlMapper = yamlMapper();
        
        JsonNode configNode = yamlMapper.readTree(yamlString);

        Config config = objectMapper.treeToValue(configNode, Config.class);

        validateBean(config);

        return config;
    }

    public static Config readConfigFromXml(String xmlString) throws JsonProcessingException, IOException {
        ObjectMapper objectMapper = mapper();
        XmlMapper xmlMapper = xmlMapper();
        
        JsonNode configNode = xmlMapper.readTree(xmlString);

        Config config = objectMapper.treeToValue(configNode, Config.class);

        validateBean(config);

        return config;
    }

    public static void parseToropassFile(final Config config) throws FileNotFoundException, IOException {
        if (config.getBackend().isPostgresLike()) {
            Postgres postgres = config.getBackend().asPostgres();
            postgres.setPassword(getPasswordFromToropassFile(
                    postgres.getToropassFile(), 
                    postgres.getHost(), 
                    postgres.getPort(), 
                    postgres.getDatabase(), 
                    postgres.getUser()));
        } else if(config.getBackend().isMySQLLike()) {
            MySQL mysql = config.getBackend().asMySQL();
            mysql.setPassword(getPasswordFromToropassFile(
                    mysql.getToropassFile(), 
                    mysql.getHost(), 
                    mysql.getPort(), 
                    mysql.getDatabase(), 
                    mysql.getUser()));
        }
    }

    private static String getPasswordFromToropassFile(String toropassFile, String host, int port, String database,
            String user) throws FileNotFoundException, IOException {
        File toroPass = new File(toropassFile);
        if (toroPass.exists() && toroPass.canRead() && toroPass.isFile()) {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(toroPass), Charsets.UTF_8));
            try {
                String line;
                int index = 0;
                while ((line = br.readLine()) != null) {
                    index++;
                    String[] toroPassChunks = line.split(":");
                    if (toroPassChunks.length != 5) {
                        LOGGER.warn("Wrong format at line " + index + " of file " + toropassFile);
                        continue;
                    }

                    if ((toroPassChunks[0].equals("*") || toroPassChunks[0].equals(host))
                            && (toroPassChunks[1].equals("*")
                                    || toroPassChunks[1].equals(String.valueOf(port)))
                            && (toroPassChunks[2].equals("*") || toroPassChunks[2].equals(database))
                            && (toroPassChunks[3].equals("*") || toroPassChunks[3].equals(user))) {
                        return toroPassChunks[4];
                    }
                }
                br.close();
            } finally {
                br.close();
            }
        }
        
        return null;
    }

	private static void mergeParam(ObjectMapper objectMapper, JsonNode configRootNode, String pathAndProp, String value)
			throws Exception {
		String path = pathAndProp.substring(0, pathAndProp.lastIndexOf("/"));
		String prop = pathAndProp.substring(pathAndProp.lastIndexOf("/") + 1);

		JsonPointer pathPointer = JsonPointer.compile(path);
		JsonNode pathNode = configRootNode.at(pathPointer);

		if (pathNode.isMissingNode() || pathNode.isNull()) {
			JsonPointer currentPointer = pathPointer;
			JsonPointer childOfCurrentPointer = null;
			List<JsonPointer> missingPointers = new ArrayList<>();
			List<JsonPointer> childOfMissingPointers = new ArrayList<>();
			do {
				if (pathNode.isMissingNode() || pathNode.isNull()) {
					missingPointers.add(0, currentPointer);
					childOfMissingPointers.add(0, childOfCurrentPointer);
				}

				childOfCurrentPointer = currentPointer;
				currentPointer = currentPointer.head();
				pathNode = configRootNode.at(currentPointer);
			} while (pathNode.isMissingNode() || pathNode.isNull());

			for (int missingPointerIndex = 0; missingPointerIndex < missingPointers.size(); missingPointerIndex++) {
				final JsonPointer missingPointer = missingPointers.get(missingPointerIndex);
				final JsonPointer childOfMissingPointer = childOfMissingPointers.get(missingPointerIndex);

				final List<JsonNode> newNodes = new ArrayList<>();

				if (pathNode.isObject()) {
					((ObjectNode) pathNode).set(missingPointer.last().getMatchingProperty(),
							createNode(childOfMissingPointer, newNodes));
				} else if (pathNode.isArray() && missingPointer.last().mayMatchElement()) {
					for (int index = ((ArrayNode) pathNode).size(); index < missingPointer.last().getMatchingIndex()
							+ 1; index++) {
						((ArrayNode) pathNode).add(createNode(childOfMissingPointer, newNodes));
					}
				} else {
					throw new RuntimeException("Cannot set param " + pathAndProp + "=" + value);
				}

				pathNode = newNodes.get(newNodes.size() - 1);
			}
		}

		ObjectNode objectNode = (ObjectNode) pathNode;
		Object valueAsObject = objectMapper.readValue(value, Object.class);
		if (valueAsObject != null) {
			JsonNode valueNode = objectMapper.valueToTree(valueAsObject);
			objectNode.set(prop, valueNode);
		} else {
			objectNode.remove(prop);
		}
	}

	private static JsonNode createNode(JsonPointer childOfPointer, List<JsonNode> newNodes) {
		JsonNode newNode;

		if (childOfPointer == null || !childOfPointer.last().mayMatchElement()) {
			newNode = JsonNodeFactory.instance.objectNode();
		} else {
			newNode = JsonNodeFactory.instance.arrayNode();
		}

		newNodes.add(newNode);

		return newNode;
	}

	public static void printYamlConfig(Config config, Console console)
			throws IOException, JsonGenerationException, JsonMappingException {
		ObjectMapper objectMapper = yamlMapper();
		ObjectWriter objectWriter = objectMapper.writer();
		printConfig(config, console, objectWriter);
	}

	public static void printXmlConfig(Config config, Console console)
			throws IOException, JsonGenerationException, JsonMappingException {
		ObjectMapper objectMapper = xmlMapper();
		ObjectWriter objectWriter = objectMapper.writer();
		objectWriter = objectWriter.withRootName("config");
		printConfig(config, console, objectWriter);
	}

	private static void printConfig(Config config, Console console, ObjectWriter objectWriter)
			throws IOException, JsonGenerationException, JsonMappingException, UnsupportedEncodingException {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(byteArrayOutputStream, false, Charsets.UTF_8.name());
		objectWriter.writeValue(printStream, config);
		console.println(byteArrayOutputStream.toString(Charsets.UTF_8.name()));
	}

	public static void printParamDescriptionFromConfigSchema(Console console, int tabs)
			throws UnsupportedEncodingException, JsonMappingException {
		ObjectMapper objectMapper = mapper();
		ResourceBundle resourceBundle = PropertyResourceBundle.getBundle("ConfigMessages");
		DescriptionFactoryWrapper visitor = new DescriptionFactoryWrapper(resourceBundle, console, tabs);
		objectMapper.acceptJsonFormatVisitor(objectMapper.constructType(Config.class), visitor);
		console.println("");
	}
	
	public static void validateBean(Config config) {
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<Config>> constraintViolations = validator.validate(config);
		if (!constraintViolations.isEmpty()) {
			StringBuilder constraintViolationExceptionMessageBuilder = new StringBuilder();
			for (ConstraintViolation<Config> constraintViolation : constraintViolations) {
				if (constraintViolationExceptionMessageBuilder.length() > 0) {
					constraintViolationExceptionMessageBuilder.append(", ");
				}
				Path path = constraintViolation.getPropertyPath();
				JsonPointer pointer = toJsonPointer(path);
				constraintViolationExceptionMessageBuilder.append(pointer.toString());
				constraintViolationExceptionMessageBuilder.append(": ");
				constraintViolationExceptionMessageBuilder.append(constraintViolation.getMessage());
			}
			throw new IllegalArgumentException(constraintViolationExceptionMessageBuilder.toString());
		}
	}

	public static JsonPointer toJsonPointer(Path path) {
		JsonPointer pointer = JsonPointer.valueOf(null);
		for (Path.Node pathNode : path) {
			if (pathNode.getIndex() != null) {
				pointer = pointer.append(JsonPointer.valueOf("/" + pathNode.getIndex()));
			} else if (pathNode.getName() != null) {
				pointer = pointer.append(JsonPointer.valueOf("/" + pathNode.getName()));
			}
		}
		return pointer;
	}

}
