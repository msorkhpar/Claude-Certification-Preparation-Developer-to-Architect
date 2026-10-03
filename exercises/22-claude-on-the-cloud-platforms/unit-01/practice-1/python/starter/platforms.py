"""One request, three front doors: the direct API, Amazon Bedrock and Google Vertex AI. See ../../statement.md."""


class PlatformError(Exception):
    """The request cannot be built for this platform. `field` names the offending part."""

    def __init__(self, field, reason):
        super().__init__(f"{field}: {reason}")
        self.field, self.reason = field, reason


def build_request(platform, model, body, config):
    # TODO: return {"method", "url", "headers", "body"} for the platform, or raise PlatformError.
    return None


def unsupported_features(platform, features):
    # TODO: return the features of the list that the platform lacks, in the order given.
    return None
