#!/usr/bin/env python3

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import parse_qs, urlparse


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        params = parse_qs(parsed.query)

        canary = params.get("canary", [""])[0]
        body = canary.encode("utf-8")

        self.send_response(200)
        self.send_header(
            "Content-Type",
            "text/plain; charset=utf-8",
        )
        self.send_header(
            "Content-Length",
            str(len(body)),
        )
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        print(
            "%s - %s"
            % (self.address_string(), format % args),
            flush=True,
        )


if __name__ == "__main__":
    server = ThreadingHTTPServer(
        ("0.0.0.0", 9090),
        Handler,
    )

    print(
        "SSRF sink listening on port 9090",
        flush=True,
    )

    server.serve_forever()