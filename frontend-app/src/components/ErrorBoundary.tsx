import {Component, ErrorInfo, ReactNode} from 'react';
import {Box, Button, Container, Paper, Typography} from '@mui/material';
import {logger} from '../utils/logger';

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

/**
 * Error boundary component to catch and handle React errors gracefully.
 */
export class ErrorBoundary extends Component<Props, State> {
    public state: State = {
        hasError: false,
        error: null,
    };

    public static getDerivedStateFromError(error: Error): State {
        return {hasError: true, error};
    }

    public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
        logger.error('Uncaught error in ErrorBoundary', error, {
            componentStack: errorInfo.componentStack,
        });
    }

    public render() {
        if (this.state.hasError) {
            return (
                    <Container maxWidth="sm">
                        <Box
                                sx={{
                                    minHeight: '100vh',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                }}
                        >
                            <Paper elevation={3} sx={{p: 4, textAlign: 'center'}}>
                                <Typography variant="h4" component="h1" gutterBottom color="error">
                                    Something went wrong
                                </Typography>
                                <Typography variant="body1" color="text.secondary" sx={{mb: 3}}>
                                    {this.state.error?.message || 'An unexpected error occurred'}
                                </Typography>
                                <Button variant="contained" onClick={this.handleReset}>
                                    Go to Home
                                </Button>
                            </Paper>
                        </Box>
                    </Container>
            );
        }

        return this.props.children;
    }

    private handleReset = () => {
        this.setState({hasError: false, error: null});
        window.location.href = '/';
    };
}

