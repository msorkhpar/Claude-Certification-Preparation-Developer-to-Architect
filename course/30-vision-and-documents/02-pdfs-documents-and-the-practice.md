# PDFs and documents: limits, cost and the practice

**Level:** Developer · **Module 30:** Vision and documents · **Page 2 of 2**
**Exams:** DV1

**After this page you can** send a PDF or a text file to Claude, name the limits that apply, estimate what a document costs and why, choose between base64, a URL and the Files API, and plan a request that carries images and documents before any call is made.

Checked against the Claude API documentation (PDF support, Files API and Vision) on 2026-10-03. The example of the previous page and the practice at the end of this one ran offline in the course container. Nothing on this page was run against the live API.

## Why it matters

Documents are where most real vision work happens: contracts, invoices, annual reports, manuals. A PDF is not a text file with pictures attached. The model gets every page as an image, plus the text extracted from it, and that decides what it can read, what it costs and what limits you hit. An application that treats a PDF as plain text loses the charts; one that treats it as 600 free pages meets a request-size error first.

## The idea

### How a PDF reaches the model

Claude works with "any standard PDF", and every active model supports PDF processing. Provide it in one of three ways: a URL, a base64 string in a `document` content block, or a `file_id` from the Files API. On Amazon Bedrock and Google Cloud only base64 sources are available, and on Microsoft Foundry the Files API is not supported for deployments hosted on Azure.

What happens next is the part to remember. The system "converts each page of the document into an image", and "the text from each page is extracted and provided alongside each page's image." Claude therefore sees text and pictures together, which is why a question about a chart or a table works. It also means PDF support "is subject to the same limitations and considerations as other vision tasks": small text, rotated pages and poor scans cause the same mistakes as a photo would.

A plain text file can go into a document block too: upload `.txt`, `.csv` or `.md` to the Files API with the MIME type `text/plain` and reference it by id. Binary formats such as `.xlsx` and `.docx` are not supported in document blocks; convert them to text or PDF first.

### Limits

| Requirement | Limit |
|---|---|
| Request size | 32 MB, varies by platform |
| Pages per request | 600, and 100 when the request's context window is under 1M tokens |
| Format | Standard PDF, no password and no encryption |

Both limits are on "the entire request payload, including any other content sent alongside PDFs". Dense PDFs, with many small-font pages, complex tables or heavy graphics, "can fill the context window before reaching the page limit", and large requests can fail before the page limit even with the Files API. The documented remedies: split the document into sections, and downsample the embedded images, "because each page is processed as an image". On Amazon Bedrock and Google Cloud, document blocks also count toward the more-than-20 threshold of the previous page.

### What a PDF costs

There is no PDF surcharge: "Standard API pricing applies with no additional PDF fees." The cost has two parts. The text part is "typically 1,500–3,000 tokens per page depending on content density". The image part follows the same visual-token arithmetic as any image, because every page is converted into one. Use token counting to estimate a specific file before sending it.

Amazon Bedrock's Converse API shows the same two parts as two modes. The documentation describes a text-extraction mode that uses roughly 1,000 tokens for a 3-page PDF and cannot analyze images or charts, and a full visual mode that uses roughly 7,000 tokens for the same file and requires citations to be enabled. Without citations, Converse falls back to basic text extraction, which is the usual reason charts seem to be missing. Those figures are the documentation's, for Claude on Amazon Bedrock (Opus 4.6 and earlier).

### Getting good results

The documentation's list is short enough to apply as a checklist: place PDFs before text in the request; use standard fonts; make sure the text is clear and legible; rotate pages upright; use the logical page numbers from the PDF viewer in prompts; split large PDFs into chunks; and enable prompt caching for repeated analysis. For high volume, process documents with the Message Batches API. To get answers tied to a source, turn on citations (module 29): for a PDF each citation is a page location, counted from 1.

### Files API: what to remember

The Files API is generally available and needs no beta header. A file is at most 500 MB, an organization holds up to 1 TB, and a file cannot be edited or renamed after upload. It can be given an expiry of 3,600 seconds (one hour) to 7,776,000 seconds (90 days) at upload and not changed afterwards. Files you upload cannot be downloaded; only files created by skills or the code execution tool can. Two rules of safe use matter more than the numbers. Uploaded files are accessible to the whole workspace, not scoped to an end user, a conversation or a session, so a multi-tenant application creates a workspace for each tenant. And "never accept file IDs from untrusted sources". A `file_id` is a capability: whoever can name it in a request in that workspace can read the file.

### The practice: plan a request before sending it

Most of the failures above are knowable before the call, which is what the practice builds. The statement is in `exercises/30-vision-and-documents/unit-01/practice-1/statement.md`, in Python, TypeScript, Java and Kotlin. You write `visual_tokens`, `resized_size` for the two tiers, a cost estimate from a token count and a price, a function that maps a returned coordinate back to the original image, and `plan_request`, which builds the content blocks (images and documents first, labels when there are several images, the question last) and refuses with a named field what the API would refuse: an unknown format, impossible dimensions, too many images or PDF pages for the model's window, more than 20 images with one over 2000 px, an image whose exact size matters that would be resized, and the Bedrock and Vertex base64-only rule with its smaller size limit. Nothing touches a network; the tests judge the plan.

## Traps

1. **Treating a PDF as text.** Charts, scans and layout reach the model as page images; text extraction alone is what a missing citations flag gives you on Bedrock Converse.
2. **Trusting the page limit alone.** The request size and the context window can stop a dense PDF first, so measure the payload and split the file.
3. **Using file ids as if they were private.** A file is visible to the whole workspace, and a file id from outside your application is not to be accepted.
4. **Sending a spreadsheet or a Word file as a document block.** Binary formats are not supported; convert to text or PDF first.

## Quiz

1. A team sends a 400-page annual report full of tables, and the call fails long before 600 pages. According to the documentation, what is the likely cause?
   - **a**: Heavy graphics and small type use up the context window first
   - **b**: Tables are rasterized at a resolution that the API charges for separately
   - **c**: PDFs above 100 pages are refused unless a beta header is present
   - **d**: Extracted text is capped at 1,000 tokens per document on every platform

2. A multi-tenant application stores every customer's uploaded contract with the Files API and keeps one file id per customer in its database. Which design choice protects the customers from one another?
   - **a**: Encrypt each id with a customer key before it is stored
   - **b**: Give each client its own workspace in the organization
   - **c**: Delete the files after each request and upload them anew
   - **d**: Give every file the shortest expiry that the API allows

3. A user asks for one summary that covers an Excel workbook and a CSV file. How should the application prepare them?
   - **a**: Attach both as document blocks exactly as they are, since the API opens every format
   - **b**: Convert each of them to a PDF, which is the only type a document block takes
   - **c**: Upload the delimited data as text/plain and convert the spreadsheet first
   - **d**: Paste the spreadsheet bytes into a text block and rely on the model to decode them

<details>
<summary>Answer key</summary>

1. **a**. The page says that dense PDFs with small-font pages or heavy graphics "can fill the context window before reaching the page limit". *b* is ruled out because "Standard API pricing applies with no additional PDF fees." *c* is ruled out because the limit is "600, and 100 when the request's context window is under 1M tokens", and no header is involved. *d* is ruled out because the 1,000-token figure belongs to a mode that "uses roughly 1,000 tokens for a 3-page PDF" on Bedrock Converse and is not a cap on documents.
2. **b**. The page says that "a multi-tenant application creates a workspace for each tenant" because files are visible to the whole workspace. *a* is ruled out because uploaded files are "accessible to the whole workspace, not scoped to an end user, a conversation or a session", whatever the id looks like. *c* is ruled out because "a file cannot be edited or renamed after upload" and a workspace boundary is not created by re-uploading. *d* is ruled out because an expiry is something you set "at upload and not changed afterwards", and it does not separate tenants.
3. **c**. The page says to upload `.txt`, `.csv` or `.md` "to the Files API with the MIME type `text/plain`", and that binary formats such as `.xlsx` and `.docx` "are not supported in document blocks". *a* is ruled out because "Binary formats such as `.xlsx` and `.docx` are not supported in document blocks". *b* is ruled out because "A plain text file can go into a document block too", so a PDF is not the only type. *d* is ruled out because the page tells you to "convert them to text or PDF first" and offers no route that pastes raw bytes into a text block.

</details>

## Module quiz

This quiz covers both pages of the module.

1. A mobile app uploads photos of receipts and the cost per request is higher than expected on a model in the high-resolution tier. Which statement from the module explains it?
   - **a**: A single image can use up to roughly three times more visual tokens there
   - **b**: Receipts can be billed as documents, which carry a flat surcharge per page
   - **c**: Every upload can be stored for a month and billed for the storage it occupies
   - **d**: The tier can charge for padding rows as separate tokens at a higher price

2. An agent returns a bounding box for a table on a scanned page, and the box sits slightly off when drawn on the original. The team divided the coordinates by the extent of the padded picture. What is the correct divisor?
   - **a**: The width and height of the original scan, taken before any resizing
   - **b**: The next multiple of 28 above the resized width and height
   - **c**: The dimensions of the image that the model actually received
   - **d**: The token limit of the tier, which is the same on both axes

3. A pipeline must stay safe if a new image source quietly produces larger pictures that Claude would shrink. Which feature of the API turns that silent change into an error?
   - **a**: A transformations setting that rejects any oversized input outright
   - **b**: The beta header that switches on high-resolution processing
   - **c**: Token counting, which refuses any image that would be resized
   - **d**: A smaller output limit, which makes resized images fail the call

4. A contract workflow sends one PDF per request and wants the same document answered from many requests over a week, without re-sending its bytes. Which combination follows the module?
   - **a**: Upload it to the Files API, refer to it by id, and keep tenants in separate workspaces
   - **b**: Embed it as base64 in every request and enable the stricter image limit
   - **c**: Host it at a public URL and pass the URL, since Bedrock prefers links
   - **d**: Convert it to GIF frames so each page counts as a single small image

<details>
<summary>Answer key</summary>

1. **a**. The first page says that "High-resolution images can use up to roughly three times more visual tokens than the same image on a standard-tier model". *b* is ruled out because "Standard API pricing applies with no additional PDF fees." *c* is ruled out because Claude does not keep uploads: "uploaded images are not stored beyond the request." *d* is ruled out because "The padding holds no content", and the cost is the token count of the image.
2. **c**. The first page says: "Always normalize or rescale by the resized dimensions, not the padded dimensions." *a* is ruled out because the point Claude returned lies in the resized picture, and "a 1920×1080 screenshot resizes to 1456×819" shows that the original size differs from it. *b* is ruled out because that is the padded size, and "The padding holds no content". *d* is ruled out because the budget is a token count, and the page says only that "the visual token limit is what determines the final size", which is not a length in pixels.
3. **a**. The first page says that setting `"transformations": {"oversized_image": "error"}` makes the API reject an image that would be resized, "with a `400`, instead of resizing it." *b* is ruled out because "High-resolution support is automatic on the listed models and requires no beta header or client-side opt-in." *c* is ruled out because "a successful count doesn't mean the image is within the Messages API's request limits". *d* is ruled out because the documented behaviour is rejection "instead of resizing it", and no output limit is involved.
4. **a**. The pages combine the Files API, where "the payload stays small" and an id replaces the bytes, with "a workspace for each tenant". *b* is ruled out because base64 resends the bytes on every request, and "a stricter per-image dimension limit applies" is a limit to stay under and not a feature. *c* is ruled out because on Amazon Bedrock and Google Cloud "only base64 sources are available", so a link does not work there. *d* is ruled out because "Animations are unsupported, and only the first frame is used."

</details>
