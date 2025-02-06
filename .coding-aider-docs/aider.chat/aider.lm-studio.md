# LM Studio Documentation

## To Use LM Studio

1. Install the Aider Chat package:
   ```bash
   python -m pip install -U aider-chat
   ```

2. Set your API key:
   - For Mac/Linux:
     ```bash
     export LM_STUDIO_API_KEY=<key>
     ```
   - For Windows:
     ```bash
     setx LM_STUDIO_API_KEY <key>
     ```

3. Set the API base URL:
   - For Mac/Linux:
     ```bash
     export LM_STUDIO_API_BASE=<url>
     ```
   - For Windows:
     ```bash
     setx LM_STUDIO_API_BASE <url>
     ```

4. Run Aider with your model:
   ```bash
   aider --model lm_studio/<your-model-name>
   ```

For more information on warnings that may occur when working with models that Aider is not familiar with, refer to the [model warnings](https://aider.chat/docs/llms/warnings.html) section.
