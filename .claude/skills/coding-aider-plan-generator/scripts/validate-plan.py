#!/usr/bin/env python3
"""
Validation script for coding-aider plans.
This script validates that generated plans conform to the expected structure.
"""

import os
import yaml
import re
from pathlib import Path

def validate_plan_structure(plan_name, plans_dir):
    """Validate that a plan has all required files with correct structure."""
    errors = []
    warnings = []

    # Check required files exist
    required_files = [
        f"{plan_name}.md",
        f"{plan_name}_checklist.md",
        f"{plan_name}_context.yaml"
    ]

    for file_name in required_files:
        file_path = plans_dir / file_name
        if not file_path.exists():
            errors.append(f"Missing required file: {file_name}")
            continue

        # Validate file content based on type
        if file_name.endswith('.md'):
            validate_markdown_file(file_path, errors, warnings)
        elif file_name.endswith('.yaml'):
            validate_yaml_file(file_path, errors, warnings)

    return errors, warnings

def validate_markdown_file(file_path, errors, warnings):
    """Validate markdown file structure."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()

        # Check for required header
        if not content.startswith('# [Coding Aider Plan]'):
            errors.append(f"{file_path.name}: Missing required header '# [Coding Aider Plan]'")

        # Check for main title
        if not re.search(r'^# .+$', content, re.MULTILINE):
            errors.append(f"{file_path.name}: Missing main title (H1)")

        # If checklist file, validate checkbox format
        if '_checklist' in file_path.name:
            checkboxes = re.findall(r'^- \[ \] .+$', content, re.MULTILINE)
            if len(checkboxes) < 3:
                warnings.append(f"{file_path.name}: Fewer than 3 checklist items found")
            if len(checkboxes) > 25:
                warnings.append(f"{file_path.name}: More than 25 checklist items - consider breaking into subplans")

    except Exception as e:
        errors.append(f"{file_path.name}: Error reading file: {e}")

def validate_yaml_file(file_path, errors, warnings):
    """Validate YAML context file structure."""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            data = yaml.safe_load(f)

        # Check required structure
        if not isinstance(data, dict):
            errors.append(f"{file_path.name}: YAML must be a dictionary")
            return

        if 'files' not in data:
            errors.append(f"{file_path.name}: Missing 'files' key")
            return

        files = data['files']
        if not isinstance(files, list):
            errors.append(f"{file_path.name}: 'files' must be a list")
            return

        # Validate each file entry
        for i, file_entry in enumerate(files):
            if not isinstance(file_entry, dict):
                errors.append(f"{file_path.name}: File entry {i} must be a dictionary")
                continue

            if 'path' not in file_entry:
                errors.append(f"{file_path.name}: File entry {i} missing 'path'")
                continue

            path = file_entry['path']
            if not isinstance(path, str) or not path.strip():
                errors.append(f"{file_path.name}: File entry {i} has invalid path")
                continue

            # Check read-only field
            if 'readOnly' in file_entry:
                if not isinstance(file_entry['readOnly'], bool):
                    errors.append(f"{file_path.name}: File entry {i} 'readOnly' must be boolean")
            else:
                warnings.append(f"{file_path.name}: File entry {i} missing 'readOnly' field")

        if len(files) == 0:
            warnings.append(f"{file_path.name}: No files listed in context")

    except yaml.YAMLError as e:
        errors.append(f"{file_path.name}: Invalid YAML syntax: {e}")
    except Exception as e:
        errors.append(f"{file_path.name}: Error reading file: {e}")

def main():
    """Main validation function."""
    import sys

    if len(sys.argv) != 2:
        print("Usage: python validate-plan.py <plan-name>")
        sys.exit(1)

    plan_name = sys.argv[1]
    plans_dir = Path(".coding-aider-plans")

    if not plans_dir.exists():
        print("Error: .coding-aider-plans directory not found")
        sys.exit(1)

    errors, warnings = validate_plan_structure(plan_name, plans_dir)

    if errors:
        print(f"❌ Validation failed for plan '{plan_name}':")
        for error in errors:
            print(f"  - {error}")

    if warnings:
        print(f"⚠️  Warnings for plan '{plan_name}':")
        for warning in warnings:
            print(f"  - {warning}")

    if not errors and not warnings:
        print(f"✅ Plan '{plan_name}' passed validation")
    elif not errors:
        print(f"✅ Plan '{plan_name}' passed validation with warnings")
    else:
        sys.exit(1)

if __name__ == "__main__":
    main()