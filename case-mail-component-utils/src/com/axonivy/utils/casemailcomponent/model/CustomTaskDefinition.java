package com.axonivy.utils.casemailcomponent.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.ivyteam.ivy.workflow.TaskDefinition;

/**
 * Custom simplified implementation of {@link TaskDefinition}
 *
 * @author ny.huynh
 */
public class CustomTaskDefinition {
	private String name;
	private String description;
	private String category;
	private String activatorRole;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the category
	 */
	public String getCategory() {
		return category;
	}

	/**
	 * @param category the category to set
	 */
	public void setCategory(String category) {
		this.category = category;
	}

	/**
	 * @return the activatorRole
	 */
	public String getActivatorRole() {
		return activatorRole;
	}

	/**
	 * @param activatorRole the activatorRole to set
	 */
	public void setActivatorRole(String activatorRole) {
		this.activatorRole = activatorRole;
	}

	/**
	 * Converts object <i>this</i> to JSON using com.fasterxml.jackson library.
	 * Opposite method is {@link #fromJson(String)}
	 *
	 * @return
	 */
	public String toJson() {
		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.writeValueAsString(this);
		} catch (final JsonProcessingException e) {
		}
		return "";
	}

	/**
	 * Constructor for object <i>this</i> from JSON using com.fasterxml.jackson
	 * library. Opposite method is {@link #toJson()}
	 *
	 * @param json
	 * @return
	 */
	public static CustomTaskDefinition fromJson(String json) {
		final ObjectMapper objectMapper = new ObjectMapper();
		try {
			return objectMapper.readValue(json, CustomTaskDefinition.class);
		} catch (final JsonProcessingException e) {
		}
		return null;
	}
}