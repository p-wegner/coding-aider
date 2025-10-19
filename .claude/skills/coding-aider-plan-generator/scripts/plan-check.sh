#!/bin/bash

# Simple validation script for coding-aider plans
# Usage: ./plan-check.sh <plan-name>

if [ $# -ne 1 ]; then
    echo "Usage: $0 <plan-name>"
    exit 1
fi

PLAN_NAME="$1"
PLANS_DIR="../.coding-aider-plans"

if [ ! -d "$PLANS_DIR" ]; then
    echo "❌ Error: .coding-aider-plans directory not found"
    exit 1
fi

ERRORS=0
WARNINGS=0

# Check required files
for file in "${PLAN_NAME}.md" "${PLAN_NAME}_checklist.md" "${PLAN_NAME}_context.yaml"; do
    if [ ! -f "$PLANS_DIR/$file" ]; then
        echo "❌ Missing required file: $file"
        ((ERRORS++))
    fi
done

# Check main plan file structure
if [ -f "$PLANS_DIR/${PLAN_NAME}.md" ]; then
    if ! grep -q "^# \[Coding Aider Plan\]" "$PLANS_DIR/${PLAN_NAME}.md"; then
        echo "❌ ${PLAN_NAME}.md: Missing required header '# [Coding Aider Plan]'"
        ((ERRORS++))
    fi

    if ! grep -q "^# .*$" "$PLANS_DIR/${PLAN_NAME}.md"; then
        echo "❌ ${PLAN_NAME}.md: Missing main title"
        ((ERRORS++))
    fi
fi

# Check checklist file structure
if [ -f "$PLANS_DIR/${PLAN_NAME}_checklist.md" ]; then
    if ! grep -q "^# \[Coding Aider Plan - Checklist\]" "$PLANS_DIR/${PLAN_NAME}_checklist.md"; then
        echo "❌ ${PLAN_NAME}_checklist.md: Missing required header"
        ((ERRORS++))
    fi

    CHECKLIST_ITEMS=$(grep -c "^- \[ \] " "$PLANS_DIR/${PLAN_NAME}_checklist.md" || echo "0")
    if [ "$CHECKLIST_ITEMS" -lt 3 ]; then
        echo "⚠️ ${PLAN_NAME}_checklist.md: Fewer than 3 checklist items found"
        ((WARNINGS++))
    fi
fi

# Check context file structure
if [ -f "$PLANS_DIR/${PLAN_NAME}_context.yaml" ]; then
    if ! grep -q "^---" "$PLANS_DIR/${PLAN_NAME}_context.yaml"; then
        echo "⚠️ ${PLAN_NAME}_context.yaml: May not be valid YAML format"
        ((WARNINGS++))
    fi

    if ! grep -q "files:" "$PLANS_DIR/${PLAN_NAME}_context.yaml"; then
        echo "❌ ${PLAN_NAME}_context.yaml: Missing 'files:' section"
        ((ERRORS++))
    fi
fi

# Summary
if [ $ERRORS -eq 0 ] && [ $WARNINGS -eq 0 ]; then
    echo "✅ Plan '$PLAN_NAME' passed validation"
elif [ $ERRORS -eq 0 ]; then
    echo "✅ Plan '$PLAN_NAME' passed validation with $WARNINGS warning(s)"
else
    echo "❌ Plan '$PLAN_NAME' failed validation with $ERRORS error(s) and $WARNINGS warning(s)"
    exit 1
fi